package org.khelekore.parjac.semantics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class ClassResourceHolder {
    private final List<Path> classPathEntries;
    private Map<String, Result> foundClasses = new HashMap<> ();

    public ClassResourceHolder (List<Path> classPathEntries) {
	this.classPathEntries = classPathEntries;
    }

    /** Find things that may be classes, this will only validate names.
     *  More detailed checks will be done once we start using classes.
     */
    public void scanClassPath () throws IOException {
	scanBootPath ();
	for (Path p : classPathEntries)
	    scan (p);
    }

    private void scanBootPath () throws IOException {
	String bootpath = System.getProperty ("sun.boot.class.path");
	String[] parts = bootpath.split (File.pathSeparator);
	for (String part : parts) {
	    scan (Paths.get (part));
	}
    }

    private void scan (Path p) throws IOException {
	if (Files.isDirectory (p))
	    scanDirectory (p);
	else if (Files.isRegularFile (p))
	    scanJar (p);
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

    public List<String> getSuperTypes (String fqn) throws IOException {
	Result r = foundClasses.get (fqn);
	if (r == null)
	    return Collections.emptyList ();
	r.ensureNodeIsLoaded ();
	return r.superTypes;
    }

    private static abstract class Result {
	private ClassNode node;
	private List<String> superTypes;

	public void ensureNodeIsLoaded () throws IOException {
	    if (node != null)
		return;
	    node = readNode ();

	    superTypes = new ArrayList<> ((node.superName != null ? 1 : 0) +
					  (node.interfaces != null ? node.interfaces.size () : 0));
	    @SuppressWarnings ("unchecked") List<String> interfaces = node.interfaces;
	    if (node.superName != null)
		superTypes.add (node.superName.replace ('/', '.'));
	    if (interfaces != null)
		interfaces.forEach (i -> superTypes.add (i.replace ('/', '.')));
	}

	public abstract ClassNode readNode () throws IOException;
    }

    private static class PathResult extends Result {
	private final Path path;

	public PathResult (Path path) {
	    this.path = path;
	}

	public ClassNode readNode () throws IOException {
	    try (InputStream is = Files.newInputStream (path)) {
		return ClassResourceHolder.readNode (is);
	    }
	}
    }

    private static class JarEntryResult extends Result {
	private final Path jarfile;
	private final String name;

	public JarEntryResult (Path jarfile, String name) {
	    this.jarfile = jarfile;
	    this.name = name;
	}

	public ClassNode readNode () throws IOException {
	    // TODO: can we cache this jarfile?
	    try (JarFile jf = new JarFile (jarfile.toFile ())) {
		JarEntry e = jf.getJarEntry (name);
		try (InputStream jis = jf.getInputStream (e)) {
		    return ClassResourceHolder.readNode (jis);
		}
	    }
	}
    }

    private static ClassNode readNode (InputStream is) throws IOException {
	ClassReader cr = new ClassReader (is);
	ClassNode cn = new ClassNode ();
	cr.accept (cn, 0);
	return cn;
    }
}
