package org.khelekore.parjac;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.khelekore.parjac.tree.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeWriter extends ClassLoader {

    public void write (TreeNode tn, Path destinationDir, DottedName packageName) {
	if (tn instanceof NormalClassDeclaration) {
	    writeClass ((NormalClassDeclaration)tn, destinationDir, packageName);
	} else if (tn instanceof EnumDeclaration) {
	    writeEnum ((EnumDeclaration)tn, destinationDir, packageName);
	} else if (tn instanceof NormalInterfaceDeclaration) {
	    writeInterface ((NormalInterfaceDeclaration)tn, destinationDir, packageName);
	} else if (tn instanceof AnnotationTypeDeclaration) {
	    writeAnnotation ((AnnotationTypeDeclaration)tn, destinationDir, packageName);
	} else {
	    throw new IllegalStateException ("Unknown type: " + tn);
	}
    }

    private void writeClass (NormalClassDeclaration cd, Path destinationDir, DottedName packageName) {
	writeDummyClass (cd.getId (), destinationDir, packageName);
    }

    private void writeEnum (EnumDeclaration cd, Path destinationDir, DottedName packageName) {
	writeDummyClass (cd.getId (), destinationDir, packageName);
    }

    private void writeInterface (NormalInterfaceDeclaration cd,
				 Path destinationDir, DottedName packageName) {
	writeDummyClass (cd.getId (), destinationDir, packageName);
    }

    private void writeAnnotation (AnnotationTypeDeclaration cd,
				  Path destinationDir, DottedName packageName) {
	writeDummyClass (cd.getId (), destinationDir, packageName);
    }

    private void writeDummyClass (String id, Path destinationDir, DottedName packageName) {
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

	Path path = getPath (destinationDir, packageName, id);
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

    private Path getPath (Path destinationDir, DottedName packageName, String id) {
	String cid = id + ".class";
	if (packageName == null)
	    return Paths.get (destinationDir.toString (), cid);
	return Paths.get (destinationDir.toString (), packageName.getPathName (), cid);
    }
}
