package org.khelekore.parjac;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;

import org.khelekore.parjac.tree.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeWriter implements TreeVisitor {
    private final Path destinationDir;
    private DottedName packageName;
    private final Deque<ClassId> classes = new ArrayDeque<> ();

    public BytecodeWriter (Path destinationDir) {
	this.destinationDir = destinationDir;
    }

    public void visit (CompilationUnit cu) {
	packageName = cu.getPackage ();
    }

    public void visit (NormalClassDeclaration c) {
	pushClass (c.getId ());
    }

    public void visit (EnumDeclaration e) {
	pushClass (e.getId ());
    }

    public void visit (NormalInterfaceDeclaration i) {
	pushClass (i.getId ());
    }

    public void visit (AnnotationTypeDeclaration a) {
	pushClass (a.getId ());
    }

    public void anonymousClass (ClassBody b) {
	pushClass (generateAnonId ());
    }

    private void pushClass (String id) {
	ClassId cid = new ClassId (id);
	classes.addLast (cid);
	cid.start ();
    }

    private String generateAnonId () {
	ClassId cid = classes.peekLast ();
	cid.anonId++;
	return Integer.toString (cid.anonId);
    }

    public void endType () {
	ClassId cid = classes.removeLast ();
	cid.write ();
    }

    public void visit (FieldDeclaration f) {
	ClassWriter cw = classes.peekLast ().cw;
	for (VariableDeclarator vd : f.getVariables ().get ()) {
	    // int access, String name, String desc, String signature, Object value)
	    FieldVisitor fw = cw.visitField (ACC_PRIVATE, vd.getId (), "I", null, null);
	    fw.visitEnd ();
	}
    }

    public void visit (MethodDeclaration m) {
	ClassWriter cw = classes.peekLast ().cw;
        // creates a MethodWriter for the method
        MethodVisitor mw = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, m.getMethodName (),
                "([Ljava/lang/String;)V", null, null);
        // pushes the 'out' field (of type PrintStream) of the System class
        mw.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
                "Ljava/io/PrintStream;");
        // pushes the "Hello World!" String constant
        mw.visitLdcInsn("Hello world!");
        // invokes the 'println' method (defined in the PrintStream class)
        mw.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);
        mw.visitInsn(RETURN);
        // this code uses a maximum of two stack elements and two local
        // variables
        mw.visitMaxs(2, 2);
        mw.visitEnd();
    }

    public void visit (Block b) {
    }

    public void endBlock () {
    }

    private class ClassId {
	private final String id;
	private String fullId;
	private final ClassWriter cw;
	private int anonId;

	public ClassId (String id) {
	    this.id = id;
	    cw = new ClassWriter (0);
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{id: " + id + ", anonId: " + anonId + "}";
	}

	public void start () {
	    fullId = getFullId ();
	    String fqn = getFQN (packageName);
	    cw.visit (V1_8, ACC_PUBLIC, fqn, null, "java/lang/Object", null);
	    addConstructor ();

	}

	public String getFQN (DottedName packageName) {
	    if (packageName == null)
		return fullId;
	    return packageName.getDotName () + "." + fullId;
	}

	public String getFullId () {
	    return classes.stream ().map (cid -> cid.id).collect (Collectors.joining ("$"));
	}

	private void addConstructor () {
	    MethodVisitor mw =
		cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);

	    // pushes the 'this' variable
	    mw.visitVarInsn(ALOAD, 0);
	    // invokes the super class constructor
	    mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
	    mw.visitInsn(RETURN);
	    // this code uses a maximum of one stack element and one local variable
	    mw.visitMaxs(1, 1);
	    mw.visitEnd();
	}

	public void write () {
	    Path path = getPath ();
	    try {
		Files.write (path, cw.toByteArray());
	    } catch (IOException e) {
		System.err.println ("Failed to create class file: " + path);
	    }
	}

	private Path getPath () {
	    String cid = fullId + ".class";
	    if (packageName == null)
		return Paths.get (destinationDir.toString (), cid);
	    return Paths.get (destinationDir.toString (), packageName.getPathName (), cid);
	}

    }
}
