package org.khelekore.parjac.semantics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class ClassResourceHolder {
    private final List<Path> classPathEntries;
    private final CompilerDiagnosticCollector diagnostics;
    private Map<String, Result> foundClasses = new HashMap<> ();

    public ClassResourceHolder (List<Path> classPathEntries,
				CompilerDiagnosticCollector diagnostics) {
	this.classPathEntries = classPathEntries;
	this.diagnostics = diagnostics;
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
	Files.walk (start).forEach (f -> {
		Path relative = start.relativize (f);
		storeName (relative.toString (), new PathResult (f));
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
	    foundClasses.put (name, r);
	}
    }

    public LookupResult hasVisibleType (String fqn) {
	Result r = foundClasses.get (fqn);
	if (r != null) {
	    try {
		r.ensureNodeIsLoaded ();
	    } catch (IOException e) {
		diagnostics.report (new NoSourceDiagnostics ("Failed to load class from: " +
							     r.getPath ()));
		r = null;
	    }
	}
	if (r == null)
	    return LookupResult.NOT_FOUND;
	return new LookupResult (true, r.node.access);
    }

    public Optional<List<String>> getSuperTypes (String fqn) throws IOException {
	Result r = foundClasses.get (fqn);
	if (r == null)
	    return Optional.empty ();
	r.ensureNodeIsLoaded ();
	return Optional.of (r.superTypes);
    }

    public int getClassModifiers (String fqn) {
	Result r = foundClasses.get (fqn);
	if (r == null)
	    throw new IllegalArgumentException ("No such class: " + fqn);
	return r.node.access;
    }

    private static abstract class Result {
	private ClassNode node;
	private List<String> superTypes;

	public synchronized void ensureNodeIsLoaded () throws IOException {
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

	public abstract String getPath ();
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

	public String getPath () {
	    return path.toString ();
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
	    // I tried to cache the jarfiles, but that made things a lot slower
	    // due to less multi threading. Do if someone can prove it makes sense
	    try (JarFile jf = new JarFile (jarfile.toFile ())) {
		JarEntry e = jf.getJarEntry (name);
		try (InputStream jis = jf.getInputStream (e)) {
		    return ClassResourceHolder.readNode (jis);
		}
	    }
	}

	public String getPath () {
	    return jarfile.toString () + "!" + name;
	}
    }

    private static ClassNode readNode (InputStream is) throws IOException {
	try {
	    ClassReader cr = new ClassReader (is);
	    ClassNode cn = new ClassNode ();
	    cr.accept (cn, 0);
	    return cn;
	} catch (RuntimeException e) {
	    throw new IOException ("Failed to read class: ", e);
	}
    }
}
