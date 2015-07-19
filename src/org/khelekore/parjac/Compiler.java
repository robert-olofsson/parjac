package org.khelekore.parjac;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.khelekore.parjac.batch.CompilationArguments;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.lexer.CharBufferLexer;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.parser.EarleyParser;
import org.khelekore.parjac.parser.JavaTreeBuilder;
import org.khelekore.parjac.parser.PredictCache;
import org.khelekore.parjac.semantics.ClassResourceHolder;
import org.khelekore.parjac.semantics.ClassSetter;
import org.khelekore.parjac.semantics.CompiledTypesHolder;
import org.khelekore.parjac.semantics.NameModifierChecker;
import org.khelekore.parjac.tree.CompilationUnit;
import org.khelekore.parjac.tree.DottedName;
import org.khelekore.parjac.tree.SyntaxTree;

/** The actual compiler
 */
public class Compiler {
    private final CompilerDiagnosticCollector diagnostics;
    private final Grammar g;
    private final PredictCache predictCache;
    private final JavaTreeBuilder treeBuilder;
    private final CompilationArguments settings;

    private CompiledTypesHolder cth;
    private ClassResourceHolder crh;

    public Compiler (CompilerDiagnosticCollector diagnostics, Grammar g,
		     CompilationArguments settings) {
	this.diagnostics = diagnostics;
	this.g = g;
	this.predictCache = new PredictCache (g);
	this.treeBuilder = new JavaTreeBuilder (g);
	this.settings = settings;
    }

    public void compile (List<Path> srcFiles) {
	long startParse = System.nanoTime ();
	List<SyntaxTree> trees = parse (srcFiles);
	long endParse = System.nanoTime ();
	if (settings.getReportTime ())
	    reportTime ("Complete parsing", startParse, endParse);
	if (diagnostics.hasError ())
	    return;

	scanClassPaths ();
	checkSemantics (trees);
	if (diagnostics.hasError ())
	    return;

	createOutputDirectories (trees, settings.getOutputDir ());
	if (diagnostics.hasError ())
	    return;

	writeClasses (trees, settings.getOutputDir ());
    }

    private List<SyntaxTree> parse (List<Path> srcFiles) {
	return
	    srcFiles.parallelStream ().
	    map (p -> parse (p)).
	    filter (p -> p != null).
	    collect (Collectors.toList ());
    }

    private SyntaxTree parse (Path path) {
	try {
	    long start = System.nanoTime ();
	    if (settings.getDebug ())
		System.out.println ("parsing: " + path);
	    ByteBuffer buf = ByteBuffer.wrap (Files.readAllBytes (path));
	    CharsetDecoder decoder = settings.getEncoding ().newDecoder ();
	    decoder.onMalformedInput (CodingErrorAction.REPORT);
	    decoder.onUnmappableCharacter (CodingErrorAction.REPORT);
	    CharBuffer charBuf = decoder.decode (buf);
	    Lexer lexer = new CharBufferLexer (charBuf);
	    EarleyParser parser = new EarleyParser (g, path, lexer, predictCache, treeBuilder,
						    diagnostics, settings.getDebug ());
	    SyntaxTree tree = parser.parse ();
	    long end = System.nanoTime ();
	    if (settings.getDebug ())
		reportTime ("Parsing " + path, start, end);
	    return tree;
	} catch (MalformedInputException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to decode text: %s using %s",
							 path, settings.getEncoding ()));
	    return null;
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to read: %s: %s", path, e));
	    return null;
	}
    }

    private void scanClassPaths () {
	try {
	    crh = new ClassResourceHolder (settings.getClassPathEntries (), diagnostics);
	    crh.scanClassPath ();
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to scan classpath: %s", e));
	}
    }

    private void checkSemantics (List<SyntaxTree> trees) {
	cth = new CompiledTypesHolder ();
	trees.parallelStream ().forEach (t -> cth.addTypes (t));

	// Fill in correct classes
	// Depending on order we may not have correct parents on first try.
	// We collect the trees that fails and tries again
	long start = System.nanoTime ();
	Queue<SyntaxTree> rest = new ConcurrentLinkedQueue<SyntaxTree> ();
	trees.parallelStream ().forEach (t -> fillInClasses (t, null, rest));
	if (!rest.isEmpty ()) {
	    Queue<SyntaxTree> rest2 = new ConcurrentLinkedQueue<SyntaxTree> ();
	    rest.parallelStream ().forEach (t -> fillInClasses (t, diagnostics, rest2));
	}
	long end = System.nanoTime ();
	if (settings.getReportTime ())
	    reportTime ("Classes filled in", start, end);
	// Check file names / class names matching and modifiers
	trees.parallelStream ().forEach (t -> checkNamesAndModifiers (t, diagnostics));
	// Check types of fields and assignments
	// Check matching methods
	// Check generics
    }

    private void fillInClasses (SyntaxTree tree,
				CompilerDiagnosticCollector diagnostics,
				Queue<SyntaxTree> rest) {
	ClassSetter cs = new ClassSetter (cth, crh, tree, diagnostics);
	cs.fillIn ();
	if (!cs.completed ())
	    rest.add (tree);
    }

    private void checkNamesAndModifiers (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	NameModifierChecker nmc = new NameModifierChecker (tree, diagnostics);
	nmc.check ();
    }

    private void createOutputDirectories (List<SyntaxTree> trees,
					  Path destinationDir) {
	Set<Path> dirs = new HashSet<> ();
	trees.stream ().
	    forEach (t -> dirs.add (getPath (destinationDir, t)));
	dirs.forEach (p -> createDirectory (p));
    }

    private Path getPath (Path dest, SyntaxTree t) {
	CompilationUnit cu = t.getCompilationUnit ();
	DottedName packageName = cu.getPackage ();
	if (packageName == null)
	    return dest;
	return Paths.get (dest.toString (), packageName.getPathName ());
    }

    private void createDirectory (Path p) {
	try {
	    Files.createDirectories (p);
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to create output directory: %s", p));
	}
    }

    private void writeClasses (List<SyntaxTree> trees, Path destinationDir) {
	trees.parallelStream ().
	    forEach (t -> writeClasses (t, destinationDir));
    }

    private void writeClasses (SyntaxTree tree, Path destinationDir) {
	BytecodeWriter w = new BytecodeWriter (tree.getOrigin (), destinationDir, cth);
	tree.getCompilationUnit ().visit (w);
    }

    private void reportTime (String type, long start, long end) {
	System.out.format ("%s, time taken: %.3f millis\n", type, (end - start) / 1.0e6);
    }
}
