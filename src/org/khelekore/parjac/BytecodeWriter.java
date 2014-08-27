package org.khelekore.parjac;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.khelekore.parjac.tree.SyntaxTree;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeWriter extends ClassLoader {

    public void write (SyntaxTree tree, Path destinationDir) {
	// TODO: when we know the class name we do not need this silly thing
	Path origin = tree.getOrigin ();
	Path srcrelative = origin.subpath (1, origin.getNameCount ());
	String filename = srcrelative.getFileName ().toString ();
	String classname = filename.replaceAll ("(?i).java$", "");
	String classfile = classname + ".class";
	Path relative =
	    Paths.get (srcrelative.getParent ().toString (), classfile);
	Path cn = Paths.get (srcrelative.getParent ().toString (), classname);
	String fqn = cn.toString ();

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

	Path path =
	    Paths.get (destinationDir.toString (), relative.toString ());

	try {
	    Files.createDirectories (path.getParent ());
	    Files.write (path, cw.toByteArray());
	} catch (IOException e) {
	    System.err.println ("Failed to create class file: " + path);
	}
    }
}
