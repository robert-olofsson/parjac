package org.khelekore.parjac.batch;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.khelekore.parjac.Compiler;
import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;

/** Program to run a batch compilation.
 */
public class Main {
    private final CompilerDiagnosticCollector diagnostics;

    public static void main (String[] args) {
	CompilerDiagnosticCollector collector = new CompilerDiagnosticCollector ();

	Main main = new Main (collector);

	main.compile (args);
	Locale locale = Locale.getDefault ();
	collector.getDiagnostics ().
	    forEach (d -> System.err.println (d.getMessage (locale)));
    }

    public Main (CompilerDiagnosticCollector diagnostics) {
	this.diagnostics = diagnostics;
    }

    public void compile (String[] args) {
	long startTime = System.nanoTime ();
	CompilationArguments settings = parseArgs (args);
	if (settings == null || diagnostics.hasError ())
	    return;

	List<Path> srcFiles = new ArrayList<> ();
	settings.getSrcDirs ().stream ().
	    forEach (p -> addFiles (p, srcFiles));

	if (diagnostics.hasError ())
	    return;

	System.out.println ("compiling " + srcFiles.size () + " files");
	System.out.println ("destination: " + settings.getOutputDir ());
	Compiler c = new Compiler (diagnostics);
	c.compile (srcFiles, settings.getOutputDir (), settings.getEncoding ());
	long endTime = System.nanoTime ();
	System.out.printf ("time taken: %.3f\n", ((endTime - startTime) / 1e9));
    }

    private CompilationArguments parseArgs (String[] args) {
	List<Path> srcDirs = new ArrayList<> ();
	Path outputDir = null;
	Charset encoding = Charset.forName ("UTF-8");
	for (int i = 0; i < args.length; i++) {
	    switch (args[i]) {
	    case "-i":
	    case "--input":
		if (hasFollowingArgExists (args, i))
		    srcDirs.add (Paths.get (args[++i]));
	        break;
	    case "-d":
	    case "--destination":
		if (hasFollowingArgExists (args, i))
		    outputDir = Paths.get (args[++i]);
	        break;
	    case "--encoding":
		if (hasFollowingArgExists (args, i)) {
		    String e = args[++i];
		    if (!Charset.isSupported (e)) {
			diagnostics.report (new NoSourceDiagnostics ("Unkonwn encoding: %s", e));
			return null;
		    }
		    encoding = Charset.forName (e);
		}
		break;
	    case "-h":
	    case "--help":
		usage ();
	        return null;
	    default:
		diagnostics.report (new NoSourceDiagnostics ("Unkonwn argument: %s", args[i]));
	    }
	}
	return new CompilationArguments (srcDirs, outputDir, encoding);
    }

    private boolean hasFollowingArgExists (String[] args, int pos) {
	if (args.length <= (pos + 1)) {
	    diagnostics.report (new NoSourceDiagnostics ("Missing argument following: %s, pos: %d",
							 args[pos], pos));
	    return false;
	}
	return true;
    }

    private void usage () {
	System.err.println ("usage: java " + getClass () +
			    " [-i|--input srcdir]+ [-d|--destination dir] " +
			    "[-h|--help]");
    }

    private void addFiles (Path p, List<Path> srcFiles) {
	if (!Files.isDirectory (p)) {
	    diagnostics.report (new NoSourceDiagnostics ("input srcdir: %s is not a directory", p));
	    return;
	}

	try {
	    srcFiles.addAll (Files.walk (p, FileVisitOption.FOLLOW_LINKS).
			     filter (Main::isJavaFile).
			     collect (Collectors.toList ()));
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to get files from: %s", p));
	}
    }

    private static boolean isJavaFile (Path p) {
	return p.getFileName ().toString ().toLowerCase ().endsWith (".java");
    }
}