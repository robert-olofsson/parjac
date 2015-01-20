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
import org.khelekore.parjac.tree.SyntaxTree;

/** The actual compiler
 */
public class Compiler {
    private final CompilerDiagnosticCollector diagnostics;
    private final Grammar g;
    private final PredictCache predictCache;
    private final JavaTreeBuilder treeBuilder;
    private final CompilationArguments settings;

    public Compiler (CompilerDiagnosticCollector diagnostics, Grammar g,
		     CompilationArguments settings) {
	this.diagnostics = diagnostics;
	this.g = g;
	this.predictCache = new PredictCache (g);
	this.treeBuilder = new JavaTreeBuilder (g);
	this.settings = settings;
    }

    public void compile (List<Path> srcFiles) {
	List<SyntaxTree> trees = parse (srcFiles);
	if (diagnostics.hasError ())
	    return;

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
	    //if (settings.getDebug ())
		System.out.println ("parsing: " + path);
	    ByteBuffer buf = ByteBuffer.wrap (Files.readAllBytes (path));
	    CharsetDecoder decoder = settings.getEncoding ().newDecoder ();
	    decoder.onMalformedInput (CodingErrorAction.REPORT);
	    decoder.onUnmappableCharacter (CodingErrorAction.REPORT);
	    CharBuffer charBuf = decoder.decode (buf);
	    Lexer lexer = new CharBufferLexer (charBuf);
	    // TODO: build tree
	    EarleyParser parser = new EarleyParser (g, path, lexer, predictCache, null /*treeBuilder*/,
						    diagnostics, settings.getDebug ());
	    SyntaxTree tree = parser.parse ();
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

    private void checkSemantics (List<SyntaxTree> trees) {
	// TODO: implement
    }

    private void createOutputDirectories (List<SyntaxTree> trees,
					  Path destinationDir) {
	Set<Path> dirs = new HashSet<> ();
	trees.stream ().
	    forEach (t -> dirs.add (Paths.get (destinationDir.toString (),
					       t.getRelativeClassName ().getParent ().toString ())));
	dirs.forEach (p -> createDirectory (p));
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
	    forEach (t -> writeClass (t, destinationDir));
    }

    private void writeClass (SyntaxTree tree, Path destinationDir) {
	BytecodeWriter w = new BytecodeWriter ();
	w.write (tree, destinationDir);
    }
}
