package org.khelekore.parjac.batch;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.khelekore.parjac.Compiler;

/** Program to run a batch compilation.
 */
public class Main {
    public static void main (String[] args) {
	Main main = new Main ();
	main.compile (args);
    }

    public void compile (String[] args) {
	CompilationArguments settings = parseArgs (args);
	if (settings == null)
	    return;

	List<Path> srcFiles = new ArrayList<> ();
	settings.getSrcDirs ().stream ().
	    forEach (p -> addFiles (p, srcFiles));

	System.out.println ("compiling " + srcFiles.size () + " files");
	System.out.println ("compiling " + srcFiles);
	System.out.println ("destination: " + settings.getOutputDir ());

	Compiler c = new Compiler ();
	c.compile (srcFiles, settings.getOutputDir ());
    }

    private CompilationArguments parseArgs (String[] args) {
	List<Path> srcDirs = new ArrayList<> ();
	Path outputDir = null;
	for (int i = 0; i < args.length; i++) {
	    switch (args[i]) {
	    case "-i":
	    case "--input":
		checkFollowingArgExists (args, i);
		srcDirs.add (Paths.get (args[++i]));
	        break;
	    case "-d":
	    case "--destination":
		checkFollowingArgExists (args, i);
		outputDir = Paths.get (args[++i]);
	        break;
	    case "-h":
	    case "--help":
		usage ();
	        return null;
	    default:
		throw new IllegalArgumentException ("Unknown argument: " +
						    args[i]);
	    }
	}
	return new CompilationArguments (srcDirs, outputDir);
    }

    private void checkFollowingArgExists (String[] args, int pos) {
	if (args.length <= (pos + 1))
	    throw new IllegalArgumentException ("Missing argument following: " +
						args[pos] + ", pos: " + pos);
    }

    private void usage () {
	System.err.println ("usage: java " + getClass () +
			    " [-i|--input srcdir]+ [-d|--destination dir] " +
			    "[-h|--help]");
    }

    private void addFiles (Path p, List<Path> srcFiles) {
	if (!Files.isDirectory (p))
	    throw new IllegalArgumentException ("input srcdir: " + p +
						" is not a directory");
	try {
	    srcFiles.addAll (Files.walk (p, FileVisitOption.FOLLOW_LINKS).
			     filter (Main::isJavaFile).
			     collect (Collectors.toList ()));
	} catch (IOException e) {
	    throw new IllegalArgumentException ("Failed to get files from: " +
						p);
	}
    }

    private static boolean isJavaFile (Path p) {
	return p.getFileName ().toString ().toLowerCase ().endsWith (".java");
    }
}