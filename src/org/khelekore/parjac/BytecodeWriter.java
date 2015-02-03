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
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeWriter implements TreeVisitor {
    private final Path destinationDir;
    private DottedName packageName;
    private final Deque<ClassId> names = new ArrayDeque<> ();

    public BytecodeWriter (Path destinationDir) {
	this.destinationDir = destinationDir;
    }

    public void visit (CompilationUnit cu) {
	packageName = cu.getPackage ();
    }

    public void visit (NormalClassDeclaration c) {
	String id = getId (c.getId ());
	writeDummyClass (id);
    }

    public void visit (EnumDeclaration e) {
	String id = getId (e.getId ());
	writeDummyClass (id);
    }

    public void visit (NormalInterfaceDeclaration i) {
	String id = getId (i.getId ());
	writeDummyClass (id);
    }

    public void visit (AnnotationTypeDeclaration a) {
	String id = getId (a.getId ());
	writeDummyClass (id);
    }

    public void anonymousClass (ClassBody b) {
	String id = getId (generateAnonId ());
	writeDummyClass (id);
    }

    private String getId (String id) {
	names.addLast (new ClassId (id));
	return names.stream ().map (cid -> cid.id).collect (Collectors.joining ("$"));
    }

    private String generateAnonId () {
	ClassId cid = names.peekLast ();
	cid.anonId++;
	return Integer.toString (cid.anonId);
    }

    public void endType () {
	names.removeLast ();
    }

    public void visit (FieldDeclaration f) {
    }

    public void visit (MethodDeclaration m) {
    }

    public void visit (Block b) {
    }

    public void endBlock () {
    }

    private void writeDummyClass (String id) {
	String fqn = getFQN (packageName, id);

        // creates a ClassWriter for the Example public class,
        // which inherits from Object
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC, fqn, null, "java/lang/Object", null);
        MethodVisitor mw =
	    cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);

        // pushes the 'this' variable
        mw.visitVarInsn(ALOAD, 0);
        // invokes the super class constructor
        mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
                false);
        mw.visitInsn(RETURN);
        // this code uses a maximum of one stack element and one local variable
        mw.visitMaxs(1, 1);
        mw.visitEnd();

        // creates a MethodWriter for the 'main' method
        mw = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main",
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

	Path path = getPath (id);
	try {
	    Files.write (path, cw.toByteArray());
	} catch (IOException e) {
	    System.err.println ("Failed to create class file: " + path);
	}
    }

    public String getFQN (DottedName packageName, String id) {
	if (packageName == null)
	    return id;
	return packageName.getDotName () + "." + id;
    }

    private Path getPath (String id) {
	String cid = id + ".class";
	if (packageName == null)
	    return Paths.get (destinationDir.toString (), cid);
	return Paths.get (destinationDir.toString (), packageName.getPathName (), cid);
    }

    private static class ClassId {
	private final String id;
	private int anonId;

	public ClassId (String id) {
	    this.id = id;
	}
    }
}
