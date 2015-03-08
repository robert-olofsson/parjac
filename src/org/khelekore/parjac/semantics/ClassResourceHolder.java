package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassResourceHolder {
    private Map<String, Result> foundClasses = new HashMap<> ();

    public void scanClassPath () throws IOException {
	URLClassLoader cl = (URLClassLoader)getClass ().getClassLoader ();
	scanRT ();
	for (URL u : cl.getURLs ())
	    scan (u);
    }

    private void scanRT () throws IOException {
	URL uo = getClass ().getClassLoader ().getResource ("java/lang/Object.class");
	JarURLConnection ju = (JarURLConnection)uo.openConnection ();
	try (JarFile jf = ju.getJarFile ()) {
	    jf.stream ().forEach (m -> storeClass (Paths.get (jf.getName ()), m));
	}
    }

    private void scan (URL u) throws IOException {
	if (u.getProtocol ().equals ("file")) {
	    Path p = Paths.get (u.getFile ());
	    if (Files.isDirectory (p))
		scanDirectory (p);
	    else if (Files.isRegularFile (p))
		scanJar (p);
	} else {
	    throw new IOException ("Unhandled classpath entry: " + u);
	}
    }

    private void scanDirectory (final Path start) throws IOException {
	Files.walkFileTree (start, new SimpleFileVisitor<Path> () {
	    @Override public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
		throws IOException {
		Path relative = start.relativize (file);
		storeName (relative.toString (), new PathResult (file));
		return FileVisitResult.CONTINUE;
	    }
	});
    }

    private void scanJar (Path jarfile) throws IOException {
	try (JarFile jf = new JarFile (jarfile.toFile ())) {
	    jf.stream ().forEach (e -> storeClass (jarfile, e));
	}
    }

    private void storeClass (Path jarfile, JarEntry e) {
	JarEntryResult r = new JarEntryResult (jarfile, e.getName ());
	storeName (e.getName (), r);
    }

    private void storeName (String name, Result r) {
	if (name.endsWith (".class")) {
	    name = name.substring (0, name.length () - 6);
	    name = name.replace ('/', '.');
	    name = name.replace ('$', '.');
	    foundClasses.put (name, r);
	}
    }

    public boolean hasType (String fqn) {
	Result r = foundClasses.get (fqn);
	if (r != null)
	    return true;
	return false;
    }

    private static interface Result {
    }

    private static class PathResult implements Result {
	private final Path path;

	public PathResult (Path path) {
	    this.path = path;
	}
    }

    private static class JarEntryResult implements Result {
	private final Path jarfile;
	private final String name;

	public JarEntryResult (Path jarfile, String name) {
	    this.jarfile = jarfile;
	    this.name = name;
	}
    }
}