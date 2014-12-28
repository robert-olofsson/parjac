package org.khelekore.parjac;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
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

import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.lexer.CharBufferLexer;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.parser.EarleyParser;
import org.khelekore.parjac.tree.SyntaxTree;

/** The actual compiler
 */
public class Compiler {
    private final CompilerDiagnosticCollector diagnostics;
    private final Grammar g;

    public Compiler (CompilerDiagnosticCollector diagnostics, Grammar g) {
	this.diagnostics = diagnostics;
	this.g = g;
    }

    public void compile (List<Path> srcFiles, Path destinationDir, Charset encoding) {
	List<SyntaxTree> trees = parse (srcFiles, encoding);
	if (diagnostics.hasError ())
	    return;

	checkSemantics (trees);
	if (diagnostics.hasError ())
	    return;

	createOutputDirectories (trees, destinationDir);
	if (diagnostics.hasError ())
	    return;

	writeClasses (trees, destinationDir);
    }

    private List<SyntaxTree> parse (List<Path> srcFiles, Charset encoding) {
	return
	    srcFiles.parallelStream ().
	    map (p -> parse (p, encoding)).
	    filter (p -> p != null).
	    collect (Collectors.toList ());
    }

    private SyntaxTree parse (Path path, Charset encoding) {
	try {
	    ByteBuffer buf = ByteBuffer.wrap (Files.readAllBytes (path));
	    CharsetDecoder decoder = encoding.newDecoder ();
	    decoder.onMalformedInput (CodingErrorAction.REPORT);
	    decoder.onUnmappableCharacter (CodingErrorAction.REPORT);
	    CharBuffer charBuf = decoder.decode (buf);
	    Lexer lexer = new CharBufferLexer (charBuf);
	    EarleyParser parser = new EarleyParser (g, path, lexer, diagnostics, false);
	    SyntaxTree tree = parser.parse ();
	    return tree;
	} catch (MalformedInputException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to decode text: %s using %s",
							 path, encoding));
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
