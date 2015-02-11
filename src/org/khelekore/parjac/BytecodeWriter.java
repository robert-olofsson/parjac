package org.khelekore.parjac;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.*;
import org.khelekore.parjac.tree.Result.TypeResult;
import org.khelekore.parjac.tree.Result.VoidResult;
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
	int mods = getModifiers (f.getModifiers ());
	for (VariableDeclarator vd : f.getVariables ().get ()) {
	    // int access, String name, String desc, String signature, Object value)
	    FieldVisitor fw = cw.visitField (mods, vd.getId (), getType (f.getType ()), null, null);
	    fw.visitEnd ();
	}
    }

    public void visit (MethodDeclaration m) {
	ClassWriter cw = classes.peekLast ().cw;
        // creates a MethodWriter for the method
	int mods = getModifiers (m.getModifiers ());
	if (hasVarargs (m))
	    mods += ACC_VARARGS;
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

    private int getModifiers (List<TreeNode> modifiers) {
	int ret = 0;
	if (modifiers != null) {
	    for (TreeNode tn : modifiers) {
		if (tn instanceof ModifierTokenType) {
		    ret += getModifier (((ModifierTokenType)tn).get ());
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

    private boolean hasVarargs (MethodDeclaration m) {
	FormalParameterList ls = m.getParameters ();
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
		    sb.append (getType (fp.getType ()));
	    }
	    LastFormalParameter lfp = fps.getLastFormalParameter ();
	    if (lfp != null) {
		sb.append ("[");
		sb.append (getType (lfp.getType ()));
	    }
	}
	sb.append (")");
    }

    private void appendResultType (TreeNode tn, StringBuilder sb) {
	if (tn instanceof Result.VoidResult) {
	    sb.append ("V");
	} else if (tn instanceof Result.TypeResult) {
	    sb.append (getType (((Result.TypeResult)tn).get ()));
	} else {
	    throw new IllegalStateException ("Unhandled result type: " + tn);
	}
    }

    private String getType (TreeNode tn) {
	if (tn instanceof PrimitiveTokenType) {
	    return getPrimitiveType (((PrimitiveTokenType)tn).get ());
	}
	// TODO: correct class type
	return "Ljava.lang.String;";
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
