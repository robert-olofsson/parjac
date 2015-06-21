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
import java.util.Set;
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
	//if (settings.getDebug ())
	    System.out.println (String.format ("Parsing completed in: %.3f millis",
					       (endParse - startParse) / 1.0e6));
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
		System.out.println (String.format ("parsed: %s in: %.3f millis",
						   path, (end - start) / 1.0e6));
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
	    crh = new ClassResourceHolder (settings.getClassPathEntries ());
	    crh.scanClassPath ();
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to scan classpath: %s", e));
	}
    }

    private void checkSemantics (List<SyntaxTree> trees) {
	cth = new CompiledTypesHolder ();
	trees.parallelStream ().forEach (t -> cth.addTypes (t));
	trees.parallelStream ().forEach (t -> fillInClasses (t, null));
	trees.parallelStream ().forEach (t -> fillInClasses (t, diagnostics));
	// TODO: implement
	// Fill in correct classes
	// Check modifiers
	// Check types of fields and assignments
	// Check matching methods
	// Check generics
    }

    private void fillInClasses (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	ClassSetter cs = new ClassSetter (cth, crh, tree, diagnostics);
	cs.fillIn ();
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
}
