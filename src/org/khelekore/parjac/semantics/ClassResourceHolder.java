package org.khelekore.parjac.semantics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.FlaggedType;
import org.khelekore.parjac.tree.TreeNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Provides information about classes from the classpath.
 */
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
	if (r != null)
	    if (loadNoCheckedException (fqn, r))
		return new LookupResult (true, r.accessFlags);
	return LookupResult.NOT_FOUND;
    }

    public Optional<List<String>> getSuperTypes (String fqn) throws IOException {
	Result r = foundClasses.get (fqn);
	if (r == null)
	    return Optional.empty ();
	loadNoCheckedException (fqn, r);
	return Optional.of (r.superTypes);
    }

    public int getClassModifiers (String fqn) {
	Result r = foundClasses.get (fqn);
	if (r == null)
	    throw new IllegalArgumentException ("No such class: " + fqn);
	loadNoCheckedException (fqn, r);
	return r.accessFlags;
    }

    public Map<String, List<MethodInformation>> getMethods (String fqn) {
	Result r = foundClasses.get (fqn);
	if (r == null)
	    throw new IllegalArgumentException ("No such class: " + fqn);
	loadNoCheckedException (fqn, r);
	return r.methods;
    }

    public FieldInformation<?> getFieldInformation (String fqn, String field) {
	AsmField af = getAsmField (fqn, field);
	if (af == null)
	    return null;
	// qwerty, probably need: new ClassType ().setFullName (fqn)?
	return new FieldInformation<AsmField> (field, af, null);
    }

    public ExpressionType getFieldType (String fqn, String field) {
	AsmField af = getAsmField (fqn, field);
	if (af == null)
	    return null;
	return af.type;
    }

    private AsmField getAsmField (String fqn, String field) {
	Result r = foundClasses.get (fqn);
	if (r == null)
	    throw new IllegalArgumentException ("No such class: " + fqn);
	loadNoCheckedException (fqn, r);
	return r.fieldTypes.get (field);
    }

    public boolean isInterface (String fqn) {
	Result r = foundClasses.get (fqn);
	if (r == null)
	    throw new IllegalArgumentException ("No such class: " + fqn);
	loadNoCheckedException (fqn, r);
	return FlagsHelper.isInterface (r.accessFlags);
    }

    private boolean loadNoCheckedException (String fqn, Result r) {
	try {
	    r.ensureNodeIsLoaded (fqn);
	    return true;
	} catch (IOException e) {
	    e.printStackTrace();
	    diagnostics.report (new NoSourceDiagnostics ("Failed to load class from: " +
							 r.getPath () + ", " + e));
	}
	return false;
    }

    private static abstract class Result {
	private boolean loaded = false;
	private String fqn;
	private String superClass;
	private List<String> superTypes;
	private int accessFlags;
	private Map<String, AsmField> fieldTypes = new HashMap<> ();
	private Map<String, List<MethodInformation>> methods = new HashMap<> ();

	public synchronized void ensureNodeIsLoaded (String fqn) throws IOException {
	    if (loaded)
		return;
	    loaded = true;
	    this.fqn = fqn;
	    readNode ();
	}

	public abstract void readNode () throws IOException;

	protected void readNode (InputStream is) throws IOException {
	    try {
		ClassReader cr = new ClassReader (is);
		ClassInfoExtractor cie = new ClassInfoExtractor (this);
		cr.accept (cie, ClassReader.SKIP_CODE);
		methods = Collections.unmodifiableMap (methods);
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
	    // due to less multi threading. Do if someone can prove it makes sense.
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
	    int size = 0;
	    if (r.superClass != null)
		size++;
	    if (interfaces != null)
		size += interfaces.length;
	    r.superTypes = new ArrayList<> (size);
	    if (r.superClass != null)
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
	    // desc is type, signature is only there if field is generic type
	    Type type = Type.getType (desc);
	    r.fieldTypes.put (name, new AsmField (access, ExpressionType.get (type), signature, value));
	    return null;
	}

	@Override public MethodVisitor visitMethod (int access, String name,
						    String desc, String signature,
						    String[] exceptions) {
	    // desc is type, signature is only there if field is generic type
	    MethodInformation mi = new MethodInformation (r.fqn, access, name, desc, signature, exceptions);
	    List<MethodInformation> ls = r.methods.get (name);
	    if (ls == null) {
		ls = new ArrayList<> ();
		r.methods.put (name, ls);
	    }
	    ls.add (mi);
	    return null;
	}
    }

    private static class AsmField implements FlaggedType {
	private final int access;
	private final ExpressionType type;
	private final String signature;
	private final Object value;

	public AsmField (int access, ExpressionType type, String signature, Object value) {
	    this.access = access;
	    this.type = type;
	    this.signature = signature;
	    this.value = value;
	}

	@Override public int getFlags () {
	    return access;
	}

	@Override public List<TreeNode> getAnnotations () {
	    return Collections.emptyList ();
	}

	@Override public ParsePosition getParsePosition() {
	    return null;
	}

	@Override public ExpressionType getExpressionType () {
	    return type;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + access + ", " + type + ", " +
		signature + ", " + value + "}";
	}
    }
}
