package org.khelekore.parjac;

import java.io.IOException;
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

public class BytecodeGenerator implements TreeVisitor {
    private final Path origin;
    private final CompiledTypesHolder cth;
    private final BytecodeWriter classWriter;
    private DottedName packageName;
    private final Deque<ClassWriterHolder> classes = new ArrayDeque<> ();
    private final Deque<MethodInfo> methods = new ArrayDeque<> ();

    public BytecodeGenerator (Path origin, CompiledTypesHolder cth,
			      BytecodeWriter classWriter) {
	this.origin = origin;
	this.cth = cth;
	this.classWriter = classWriter;
    }

    @Override public boolean visit (CompilationUnit cu) {
	packageName = cu.getPackage ();
	return true;
    }

    @Override public boolean visit (NormalClassDeclaration c) {
	pushClass (c);
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	pushClass (e);
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	pushClass (i);
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	pushClass (a);
	return true;
    }

    @Override public boolean anonymousClass (ClassType ct, ClassBody b) {
	pushClass (b);
	return true;
    }

    @Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	endType ();
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

    @Override public boolean visit (ConstructorDeclaration c) {
	ClassWriter cw = classes.peekLast ().cw;

	int mods = c.getFlags ();
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
	return true;
    }

    @Override public void visit (FieldDeclaration f) {
	ClassWriter cw = classes.peekLast ().cw;
	int mods = f.getFlags ();
	for (VariableDeclarator vd : f.getVariables ().get ()) {
	    // int access, String name, String desc, String signature, Object value
	    FieldVisitor fw = cw.visitField (mods, vd.getId (), getType (f.getType ()), null, null);
	    fw.visitEnd ();
	}
    }

    @Override public boolean visit (MethodDeclaration m) {
	ClassWriter cw = classes.peekLast ().cw;
        // creates a MethodWriter for the method
	int mods = m.getFlags ();
	if (hasVarargs (m.getParameters ()))
	    mods |= ACC_VARARGS;
	StringBuilder sb = new StringBuilder ();
	appendSignature (m, sb);
        MethodInfo mi = new MethodInfo (cw.visitMethod(mods, m.getMethodName (), sb.toString (), null, null));
	methods.addLast (mi);

	/* Hello world example
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
	*/
	return true;
    }

    @Override public void endMethod (MethodDeclaration m) {
	MethodInfo mi = methods.removeLast ();
	mi.mv.visitMaxs (mi.maxStack, mi.maxLocals);
        mi.mv.visitEnd ();
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
	    sb.append (ct.getFullName ());
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

    @Override public boolean visit (Block b) {
	return true;
    }

    @Override public void endBlock () {
    }

    @Override public boolean visit (ReturnStatement r) {
	return true;
    }

    @Override public void endReturn (ReturnStatement r) {
	MethodInfo mi = methods.peekLast ();
	if (r.hasExpression ()) {
	    mi.mv.visitInsn (IRETURN);
	} else {
	    mi.mv.visitInsn (RETURN);
	}
    }

    @Override public void visit (IntLiteral i) {
	MethodInfo mi = methods.peekLast ();
	mi.mv.visitLdcInsn (i.get ());
	mi.maxStack++;
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
		flags = ncd.getAccessFlags ();
	    } else if (tn instanceof EnumDeclaration) {
		EnumDeclaration ed = (EnumDeclaration)tn;
		supername = "java.lang.Enum<" + ed.getId () + ">";
		flags = (ed.getAccessFlags () | ACC_FINAL);
	    } else if (tn instanceof NormalInterfaceDeclaration) {
		NormalInterfaceDeclaration i = (NormalInterfaceDeclaration)tn;
		flags = i.getAccessFlags () | ACC_INTERFACE;
	    }
	    // TODO: handle interface flags
	    if (origin != null)
		cw.visitSource (origin.getFileName ().toString (), null);
	    cw.visit (V1_8, flags, fqn, /*signature*/null, supername, /*interfaces */null);
	}

	public void write () {
	    Path path = getPath ();
	    try {
		classWriter.write (path, cw.toByteArray ());
	    } catch (IOException e) {
		System.err.println ("Failed to create class file: " + path);
	    }
	}

	private Path getPath () {
	    String cid = cth.getFilename (tn) + ".class";
	    if (packageName == null)
		return Paths.get (cid);
	    return Paths.get (packageName.getPathName (), cid);
	}
    }

    private static class MethodInfo {
	public final MethodVisitor mv;
	public int maxStack = 0;
	public int maxLocals = 0;

	public MethodInfo (MethodVisitor mv) {
	    this.mv = mv;
	}
    }
}
