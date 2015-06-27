package org.khelekore.parjac;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.semantics.CompiledTypesHolder;
import org.khelekore.parjac.tree.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeWriter implements TreeVisitor {
    private final Path origin;
    private final Path destinationDir;
    private final CompiledTypesHolder cth;
    private DottedName packageName;
    private final Deque<ClassWriterHolder> classes = new ArrayDeque<> ();

    public BytecodeWriter (Path origin, Path destinationDir, CompiledTypesHolder cth) {
	this.origin = origin;
	this.destinationDir = destinationDir;
	this.cth = cth;
    }

    @Override public void visit (CompilationUnit cu) {
	packageName = cu.getPackage ();
    }

    @Override public void visit (NormalClassDeclaration c) {
	pushClass (c);
    }

    @Override public void visit (EnumDeclaration e) {
	pushClass (e);
    }

    @Override public void visit (NormalInterfaceDeclaration i) {
	pushClass (i);
    }

    @Override public void visit (AnnotationTypeDeclaration a) {
	pushClass (a);
    }

    @Override public void anonymousClass (ClassType ct, ClassBody b) {
	pushClass (b);
    }

    private void pushClass (TreeNode tn) {
	ClassWriterHolder cid = new ClassWriterHolder (tn);
	classes.addLast (cid);
	cid.start ();
    }

    @Override public void endType () {
	ClassWriterHolder cid = classes.removeLast ();
	cid.write ();
    }

    @Override public void visit (ConstructorDeclaration c) {
	ClassWriter cw = classes.peekLast ().cw;

	int mods = getModifiers (c.getModifiers ());
	if (hasVarargs (c.getParameters ()))
	    mods |= ACC_VARARGS;

	StringBuilder sb = new StringBuilder ();
	appendParameters (c.getParameters (), sb);
	sb.append ("V");
	MethodVisitor mw =
	    cw.visitMethod (mods, "<init>", sb.toString (), null, null);

	// pushes the 'this' variable
	mw.visitVarInsn(ALOAD, 0);
	// invokes the super class constructor
	mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
	mw.visitInsn(RETURN);
	// this code uses a maximum of one stack element and one local variable
	mw.visitMaxs(1, 1);
	mw.visitEnd();
    }

    @Override public void visit (FieldDeclaration f) {
	ClassWriter cw = classes.peekLast ().cw;
	int mods = getModifiers (f.getModifiers ());
	for (VariableDeclarator vd : f.getVariables ().get ()) {
	    // int access, String name, String desc, String signature, Object value
	    FieldVisitor fw = cw.visitField (mods, vd.getId (), getType (f.getType ()), null, null);
	    fw.visitEnd ();
	}
    }

    @Override public void visit (MethodDeclaration m) {
	ClassWriter cw = classes.peekLast ().cw;
        // creates a MethodWriter for the method
	int mods = getModifiers (m.getModifiers ());
	if (hasVarargs (m.getParameters ()))
	    mods |= ACC_VARARGS;
	StringBuilder sb = new StringBuilder ();
	appendSignature (m, sb);
        MethodVisitor mw = cw.visitMethod(mods, m.getMethodName (), sb.toString (), null, null);
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

    private boolean hasVarargs (FormalParameterList ls) {
	if (ls != null) {
	    NormalFormalParameterList fps = ls.getParameters ();
	    LastFormalParameter lfp = fps.getLastFormalParameter ();
	    return lfp != null;
	}
	return false;
    }

    private void appendSignature (MethodDeclaration m, StringBuilder sb) {
	appendParameters (m.getParameters (), sb);
	appendResultType (m.getResult (), sb);
    }

    private void appendParameters (FormalParameterList ls, StringBuilder sb) {
	sb.append ("(");
	if (ls != null) {
	    NormalFormalParameterList fps = ls.getParameters ();
	    List<FormalParameter> args = fps.getFormalParameters ();
	    if (args != null) {
		for (FormalParameter fp : args)
		    appendType (fp.getType (), sb);
	    }
	    LastFormalParameter lfp = fps.getLastFormalParameter ();
	    if (lfp != null) {
		sb.append ("[");
		appendType (lfp.getType (), sb);
	    }
	}
	sb.append (")");
    }

    private void appendResultType (TreeNode tn, StringBuilder sb) {
	if (tn instanceof Result.VoidResult) {
	    sb.append ("V");
	} else if (tn instanceof Result.TypeResult) {
	    appendType (((Result.TypeResult)tn).get (), sb);
	} else {
	    throw new IllegalStateException ("Unhandled result type: " + tn);
	}
    }

    private String getType (TreeNode tn) {
	StringBuilder sb = new StringBuilder ();
	appendType (tn, sb);
	return sb.toString ();
    }

    private void appendType (TreeNode tn, StringBuilder sb) {
	if (tn instanceof PrimitiveTokenType) {
	    sb.append (getPrimitiveType (((PrimitiveTokenType)tn).get ()));
	} else if (tn instanceof ClassType) {
	    ClassType ct = (ClassType)tn;
	    sb.append ("L");
	    String fqn = ct.getFullName ();
	    sb.append (fqn != null ? fqn : "java.lang.String"); // TODO: remove null check
	    sb.append (";");
	} else {
	    UnannArrayType at = (UnannArrayType)tn;
	    for (int i = 0, s = at.getDims ().get ().size (); i < s; i++)
		sb.append ("[");
	    appendType (at.getType (), sb);
	}
    }

    private String getPrimitiveType (Token t) {
	switch (t) {
	case BYTE:
	    return "B";
	case SHORT:
	    return "S";
	case INT:
	    return "I";
	case LONG:
	    return "J";
	case CHAR:
	    return "C";
	case FLOAT:
	    return "F";
	case DOUBLE:
	    return "D";
	case BOOLEAN:
	    return "Z";
	case VOID:
	    return "V";
	default:
	    throw new IllegalStateException ("Not a primitive type: " + t);
	}
    }

    @Override public void visit (Block b) {
    }

    @Override public void endBlock () {
    }

    private class ClassWriterHolder {
	private final TreeNode tn;
	private final ClassWriter cw;

	public ClassWriterHolder (TreeNode tn) {
	    this.tn = tn;
	    cw = new ClassWriter (0);
	}

	public void start () {
	    // TODO: we need to be careful about . and $ in generated names
	    String fqn = cth.getFullName (tn);
	    String supername = "java/lang/Object";
	    int flags = 0;
	    if (tn instanceof NormalClassDeclaration) {
		NormalClassDeclaration ncd = (NormalClassDeclaration)tn;
		ClassType ct = ncd.getSuperClass ();
		if (ct != null) {
		    supername = ct.getFullName ().replace ('.', '/');
		}
		flags = getModifiers (ncd.getModifiers ());
	    } else if (tn instanceof EnumDeclaration) {
		EnumDeclaration ed = (EnumDeclaration)tn;
		supername = "java.lang.Enum<" + ed.getId () + ">";
		flags = ACC_FINAL;
	    }
	    if (origin != null)
		cw.visitSource (origin.getFileName ().toString (), null);
	    cw.visit (V1_8, flags, fqn, null, supername, null);
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
	    String cid = cth.getFilename (tn) + ".class";
	    if (packageName == null)
		return Paths.get (destinationDir.toString (), cid);
	    return Paths.get (destinationDir.toString (), packageName.getPathName (), cid);
	}
    }

    private int getModifiers (List<TreeNode> modifiers) {
	int ret = 0;
	if (modifiers != null) {
	    for (TreeNode tn : modifiers) {
		if (tn instanceof ModifierTokenType) {
		    ret |= getModifier (((ModifierTokenType)tn).get ());
		}
	    }
	}
	return ret;
    }

    private int getModifier (Token t) {
	switch (t) {
	case PUBLIC:
	    return ACC_PUBLIC;
	case PROTECTED:
	    return ACC_PROTECTED;
	case PRIVATE:
	    return ACC_PRIVATE;
	case ABSTRACT:
	    return ACC_ABSTRACT;
	case STATIC:
	    return ACC_STATIC;
	case FINAL:
	    return ACC_FINAL;
	case STRICTFP:
	    return ACC_STRICT;
	case TRANSIENT:
	    return ACC_TRANSIENT;
	case VOLATILE:
	    return ACC_VOLATILE;
	case SYNCHRONIZED:
	    return ACC_SYNCHRONIZED;
	case NATIVE:
	    return ACC_NATIVE;
	case DEFAULT:
	    // hmm?
	default:
	    throw new IllegalStateException ("Got unexpected token: " + t);
	}
    }
}
