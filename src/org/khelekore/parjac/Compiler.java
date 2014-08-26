package org.khelekore.parjac;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import org.khelekore.parjac.tree.SyntaxTree;

/** The actual compiler
 */
public class Compiler {
    private static final Charset UTF8 = Charset.forName ("UTF-8");

    public void compile (List<Path> srcFiles, Path destinationDir) {
	List<SyntaxTree> trees =
	    srcFiles.parallelStream ().
	    map (p -> parse (p)).
	    collect (Collectors.toList ());
    }

    private SyntaxTree parse (Path p) {
	try {
	    List<String> lines = Files.readAllLines (p, UTF8);
	    return new SyntaxTree (p);
	} catch (IOException e) {
	    System.err.println ("Failed to read: " + p);
	    return null;
	}
    }
}
