package org.khelekore.parjac;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.DiagnosticListener;
import org.khelekore.parjac.tree.SyntaxTree;

/** The actual compiler
 */
public class Compiler {
    private static final Charset UTF8 = Charset.forName ("UTF-8");

    private final CompilerDiagnosticCollector diagnostics;

    public Compiler (CompilerDiagnosticCollector diagnostics) {
	this.diagnostics = diagnostics;
    }

    public void compile (List<Path> srcFiles, Path destinationDir) {
	List<SyntaxTree> trees = parse (srcFiles);
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

    private List<SyntaxTree> parse (List<Path> srcFiles) {
	return
	    srcFiles.parallelStream ().
	    map (p -> parse (p)).
	    filter (p -> p != null).
	    collect (Collectors.toList ());
    }

    private SyntaxTree parse (Path p) {
	try {
	    List<String> lines = Files.readAllLines (p, UTF8);
	    return new SyntaxTree (p);
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to read: %s", p));
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
