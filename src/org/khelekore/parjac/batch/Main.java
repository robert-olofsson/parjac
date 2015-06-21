package org.khelekore.parjac.batch;

import java.io.File;
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
import org.khelekore.parjac.JavaGrammarHelper;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.khelekore.parjac.grammar.Grammar;

/** Program to run a batch compilation.
 */
public class Main {
    private final CompilerDiagnosticCollector diagnostics;

    public static void main (String[] args) throws IOException {
	if (args.length == 0) {
	    usage ();
	    return;
	}

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

    public void compile (String[] args) throws IOException {
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

	Grammar g = JavaGrammarHelper.getValidatedJavaGrammar ();
	Compiler c = new Compiler (diagnostics, g, settings);
	c.compile (srcFiles);
	long endTime = System.nanoTime ();
	System.out.printf ("time taken: %.3f\n", ((endTime - startTime) / 1e9));
    }

    private CompilationArguments parseArgs (String[] args) {
	List<Path> srcDirs = new ArrayList<> ();
	Path outputDir = null;
	Charset encoding = Charset.forName ("UTF-8");
	List<Path> classPathEntries = new ArrayList<> ();
	boolean debug = false;
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
			diagnostics.report (new NoSourceDiagnostics ("Unknown encoding: %s", e));
			return null;
		    }
		    encoding = Charset.forName (e);
		}
		break;
	    case "--debug":
		debug = true;
		break;
	    case "-h":
	    case "--help":
		usage ();
		return null;
	    case "-cp":
	    case "-classpath":
		if (hasFollowingArgExists (args, i)) {
		    String p = args[++i];
		    splitToClasspaths (classPathEntries, p);
		}
	        break;
	    default:
		diagnostics.report (new NoSourceDiagnostics ("Unknown argument: \"%s\" " +
							     "add \"--help\" for usage", args[i]));
		return null;
	    }
	}
	CompilationArguments ca =
	    new CompilationArguments (srcDirs, outputDir, encoding, classPathEntries, debug);
	ca.validate (diagnostics);
	if (diagnostics.hasError ()) {
	    System.err.println ("Invalid arguments, use \"--help\" for usage.\nProblems found:");
	    return null;
	}
	return ca;
    }

    private boolean hasFollowingArgExists (String[] args, int pos) {
	if (args.length <= (pos + 1)) {
	    diagnostics.report (new NoSourceDiagnostics ("Missing argument following: %s, pos: %d",
							 args[pos], pos));
	    return false;
	}
	return true;
    }

    private void splitToClasspaths (List<Path> classPathEntries, String classpath) {
	String[] pathParts = classpath.split (File.pathSeparator);
	for (String s : pathParts) {
	    if (s.endsWith ("/*") || s.endsWith ("\\*")) {
		try {
		    findAllJars (classPathEntries, s);
		} catch (IOException e) {
		    diagnostics.report (new NoSourceDiagnostics ("Failed to find jars for: %s (%s)",
								 s, classpath));
		}
	    } else {
		Path p = Paths.get (s);
		if (!Files.exists (p)) {
		    diagnostics.report (new NoSourceDiagnostics ("Non existing classpath: %s (%s)",
								 s, classpath));
		} else {
		    classPathEntries.add (p);
		}
	    }
	}
    }

    private void findAllJars (List<Path> classPathEntries, String dir) throws IOException {
	Path parent = Paths.get (dir).getParent (); // remove the * part
	Files.list (parent).filter (p -> isJar (p)).forEach (p -> classPathEntries.add (p));
    }

    private static boolean isJar (Path p) {
	return Files.isRegularFile (p) &&
	    p.getFileName ().toString ().toLowerCase ().endsWith (".jar");
    }

    private static void usage () {
	System.err.println ("usage: java " + Main.class.getName () +
			    " [-cp <path>] [-classpath <path>]" + // same thing
			    " [--encoding encoding]" +
			    " [-i|--input srcdir]+ [-d|--destination dir]" +
			    " [--debug] [-h|--help]");
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