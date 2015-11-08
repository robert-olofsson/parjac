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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
	return new LookupResult (true, r.accessFlags);
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
	return r.accessFlags;
    }

    private static abstract class Result {
	private String superClass;
	private List<String> superTypes;
	private int accessFlags;

	public synchronized void ensureNodeIsLoaded () throws IOException {
	    if (superClass != null)
		return;
	    readNode ();
	}

	public abstract void readNode () throws IOException;

	protected void readNode (InputStream is) throws IOException {
	    try {
		ClassReader cr = new ClassReader (is);
		ClassInfoExtractor cie = new ClassInfoExtractor (this);
		cr.accept (cie, ClassReader.SKIP_CODE);
	    } catch (RuntimeException e) {
		throw new IOException ("Failed to read class: ", e);
	    }
	}

	public abstract String getPath ();
    }

    private static class PathResult extends Result {
	private final Path path;

	public PathResult (Path path) {
	    this.path = path;
	}

	@Override public void readNode () throws IOException {
	    try (InputStream is = Files.newInputStream (path)) {
		readNode (is);
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

	@Override public void readNode () throws IOException {
	    // I tried to cache the jarfiles, but that made things a lot slower
	    // due to less multi threading. Do if someone can prove it makes sense
	    try (JarFile jf = new JarFile (jarfile.toFile ())) {
		JarEntry e = jf.getJarEntry (name);
		try (InputStream jis = jf.getInputStream (e)) {
		    readNode (jis);
		}
	    }
	}

	public String getPath () {
	    return jarfile.toString () + "!" + name;
	}
    }

    private static class ClassInfoExtractor extends ClassVisitor {
	private final Result r;

	public ClassInfoExtractor (Result r) {
	    super (Opcodes.ASM5);
	    this.r = r;
	}

	@Override public void visit (int version, int access, String name,
				     String signature, String superName,
				     String[] interfaces) {
	    if (superName != null) // java.lang.Object have null superName
		r.superClass = superName.replace ('/', '.');
	    r.accessFlags = access;
	    r.superTypes = new ArrayList<> (interfaces == null ? 1 : 1 + interfaces.length);
	    r.superTypes.add (r.superClass);
	    if (interfaces != null) {
		for (String i : interfaces)
		    r.superTypes.add (i.replace ('/', '.'));
	    }
	}

	@Override public void visitSource (String source, String debug) {
	}

	@Override public void visitEnd () {
	}

	@Override public void visitInnerClass (String name, String outerName,
					       String innerName, int access) {
	}

	@Override public FieldVisitor visitField (int access, String name, String desc,
						  String signature, Object value) {
	    return null;
	}

	@Override public MethodVisitor visitMethod (int access, String name,
						    String desc, String signature,
						    String[] exceptions) {
	    return null;
	}
    }
}
