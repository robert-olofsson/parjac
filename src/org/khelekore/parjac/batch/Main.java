package org.khelekore.parjac.batch;

import java.nio.file.Files;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

/** Batch compiler.
 */
public class Main {
    public static void main (String[] args) {
	Main main = new Main ();
	main.compile (args);
    }

    public void compile (String[] args) {
	if (args.length < 2) {
	    usage ();
	    return;
	}
	CompilationArguments settings = parseArgs (args);

	List<Path> srcFiles = new ArrayList<> ();
	settings.getSrcDirs ().stream ().
	    forEach (p -> addFiles (p, srcFiles));

	System.out.println ("compiling " + srcFiles.size () + " files");
	System.out.println ("destination: " + settings.getOutputDir ());
    }

    private void usage () {
	System.err.println ("usage: java " + getClass () +
			    " [-i|--input srcdir]+ [-d|--destination dir]");
    }

    private CompilationArguments parseArgs (String[] args) {
	List<Path> srcDirs = new ArrayList<> ();
	Path outputDir = null;
	for (int i = 0; i < args.length; i++) {
	    switch (args[i]) {
	    case "-i":
	    case "--input":
		srcDirs.add (Paths.get (args[++i]));
	        break;
	    case "-d":
	    case "--destination":
		outputDir = Paths.get (args[++i]);
	        break;
	    default:
		throw new IllegalArgumentException ("Unknown argument: " +
						    args[i]);
	    }
	}
	return new CompilationArguments (srcDirs, outputDir);
    }

    private void addFiles (Path p, List<Path> srcFiles) {
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
	return p.toString ().toLowerCase ().endsWith (".java");
    }
}