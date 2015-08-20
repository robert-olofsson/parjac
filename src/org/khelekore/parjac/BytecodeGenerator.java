package org.khelekore.parjac;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.semantics.ClassInformationProvider;
import org.khelekore.parjac.tree.*;
import org.khelekore.parjac.tree.Result.TypeResult;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeGenerator implements TreeVisitor {
    private final Path origin;
    private final ClassInformationProvider cip;
    private final BytecodeWriter classWriter;
    private DottedName packageName;
    private final Deque<ClassWriterHolder> classes = new ArrayDeque<> ();
    private final Deque<MethodInfo> methods = new ArrayDeque<> ();

    public BytecodeGenerator (Path origin, ClassInformationProvider cip,
			      BytecodeWriter classWriter) {
	this.origin = origin;
	this.cip = cip;
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

    @Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
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
	MethodInfo mi = new MethodInfo (new Result.VoidResult (null),
					cw.visitMethod (mods, "<init>", sb.toString (), null, null));
	methods.addLast (mi);
	return true;
    }
    @Override public void endConstructor (ConstructorDeclaration c) {
	MethodInfo mi = methods.removeLast ();
	mi.mv.visitInsn (RETURN);
	mi.mv.visitMaxs (0, 0);
        mi.mv.visitEnd ();
    }

    @Override public boolean visit (ConstructorBody cb) {
	MethodInfo mi = methods.peekLast ();
	mi.mv.visitVarInsn (ALOAD, 0); // pushes "this"
	ExplicitConstructorInvocation eci = cb.getConstructorInvocation ();
	if (eci == null) {
	    mi.mv.visitMethodInsn (INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
	}
	return true;
    }

    @Override public void visit (ExplicitConstructorInvocation eci) {
	MethodInfo mi = methods.peekLast ();
	ClassWriterHolder cid = classes.peekLast ();
	SuperAndFlags saf = cid.getSuperAndFlags ();
	// TODO: the signature here is borked, need to get signature from super class
	mi.mv.visitMethodInsn (INVOKESPECIAL, saf.supername, "<init>", "()V", false);
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
        MethodInfo mi = new MethodInfo (m.getResult (),
					cw.visitMethod (mods, m.getMethodName (),
							sb.toString (), null, null));
	methods.addLast (mi);
	return true;
    }

    @Override public void endMethod (MethodDeclaration m) {
	MethodInfo mi = methods.removeLast ();
	if (m.getResult () instanceof Result.VoidResult && methodMissingReturn (m)) {
	    mi.mv.visitInsn (RETURN);
	}
	mi.mv.visitMaxs (0, 0);
        mi.mv.visitEnd ();
    }

    private boolean methodMissingReturn (MethodDeclaration md) {
	MethodBody b = md.getBody ();
	return !b.isEmpty () && b.getBlock ().lastIsNotThrowOrReturn ();
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
	    appendType (((Result.TypeResult)tn).getReturnType (), sb);
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
	    sb.append (ct.getSlashName ());
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
	    Result.TypeResult tr = (TypeResult)mi.result;
	    TreeNode tn = tr.getReturnType ();
	    if (tn instanceof PrimitiveTokenType) {
		PrimitiveTokenType ptt = (PrimitiveTokenType)tn;
		Token t = ptt.get ();
		switch (t) {
		case BOOLEAN:
		case BYTE:
		case SHORT:
		case CHAR:
		case INT:
		    mi.mv.visitInsn (IRETURN); break;
		case LONG: mi.mv.visitInsn (LRETURN); break;
		case DOUBLE: mi.mv.visitInsn (DRETURN); break;
		case FLOAT: mi.mv.visitInsn (FRETURN); break;
		default: throw new IllegalArgumentException ("Got strange token: " + t);
		}
	    } else {
		mi.mv.visitInsn (ARETURN);
	    }
	} else {
	    mi.mv.visitInsn (RETURN);
	}
    }

    @Override public void visit (IntLiteral iv) {
	// We do not handle init blocks yet.
	if (methods.isEmpty ())
	    return;

	MethodInfo mi = methods.peekLast ();
	int i = iv.get ();
	if (i >= -1 && i <= 5) {
	    mi.mv.visitInsn (ICONST_M1 + 1 + i);
	} else if (i >= -128 && i <= 127) {
	    mi.mv.visitIntInsn (BIPUSH, i);
	} else if (i >= -32768 && i <= 32767) {
	    mi.mv.visitIntInsn (SIPUSH, i);
	} else {
	    mi.mv.visitLdcInsn (i);
	}
    }

    @Override public void visit (DoubleLiteral dv) {
	// We do not handle init blocks yet.
	if (methods.isEmpty ())
	    return;

	MethodInfo mi = methods.peekLast ();
	double d = dv.get ();
	if (d == 0) {
	    mi.mv.visitInsn (DCONST_0);
	} else if (d == 1.0) {
	    mi.mv.visitInsn (DCONST_1);
	} else {
	    mi.mv.visitLdcInsn (d);
	}
    }

    @Override public void visit (FloatLiteral fv) {
	// We do not handle init blocks yet.
	if (methods.isEmpty ())
	    return;

	MethodInfo mi = methods.peekLast ();
	float f = fv.get ();
	if (f == 0) {
	    mi.mv.visitInsn (FCONST_0);
	} else if (f == 1.0f) {
	    mi.mv.visitInsn (FCONST_1);
	} else if (f == 2.0f) {
	    mi.mv.visitInsn (FCONST_2);
	} else {
	    mi.mv.visitLdcInsn (f);
	}
    }

    @Override public void visit (StringLiteral sv) {
	// We do not handle init blocks yet.
	if (methods.isEmpty ())
	    return;
	MethodInfo mi = methods.peekLast ();
	mi.mv.visitLdcInsn (sv.get ());
    }

    @Override public void visit (BooleanLiteral bv) {
	// We do not handle init blocks yet.
	if (methods.isEmpty ())
	    return;
	MethodInfo mi = methods.peekLast ();
	boolean f = bv.get ();
	mi.mv.visitInsn (f ? ICONST_1 : ICONST_0);
    }

    @Override public void visit (NullLiteral nv) {
	// We do not handle init blocks yet.
	if (methods.isEmpty ())
	    return;
	MethodInfo mi = methods.peekLast ();
	mi.mv.visitInsn (ACONST_NULL);
    }

    @Override public void visit (Assignment a) {
	// We do not handle init blocks yet.
	if (methods.isEmpty ())
	    return;
	// TODO: something like this
	/*
	MethodInfo mi = methods.peekLast ();
	mi.mv.visitIntInsn (ALOAD, 0);
	a.rhs ().visit (this);
	ClassWriterHolder cwh = classes.peekLast ();
	String fqn = cth.getFullName (cwh.tn);
	// TODO: name and type need to be set correctly
	mi.mv.visitFieldInsn (PUTFIELD, fqn, "a", "I");
	*/
    }

    @Override public boolean visit (MethodInvocation m) {
	TreeNode on = m.getOn ();
	if (on != null) {
	    on.visit (this);
	}
	ArgumentList al = m.getArgumentList ();
	if (al != null) {
	    al.visit (this);
	}
	MethodInfo mi = methods.peekLast ();
	mi.mv.visitMethodInsn (INVOKEVIRTUAL, "java/io/PrintStream", m.getId (),
			       "(Ljava/lang/String;)V", false);
	return false;
    }

    @Override public boolean visit (FieldAccess f) {
	TreeNode tn = f.getFrom ();
	if (tn instanceof ClassType) {
	    ClassType ct = (ClassType)tn;
	    String classSignature = ct.getFullName ().replace ('.', '/');
	    MethodInfo mi = methods.peekLast ();
	    mi.mv.visitFieldInsn (GETSTATIC, classSignature, f.getFieldId (),
				  "Ljava/io/PrintStream;");
	    return false;
	} else {
	    return true;
	}
    }

    private class ClassWriterHolder {
	private final TreeNode tn;
	private final ClassWriter cw;

	public ClassWriterHolder (TreeNode tn) {
	    this.tn = tn;
	    cw = new ClassWriter (ClassWriter.COMPUTE_FRAMES);
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{tn: " + tn + "}";
	}

	public void start () {
	    String fqn = cip.getFullName (tn);
	    SuperAndFlags saf = getSuperAndFlags ();
	    if (origin != null)
		cw.visitSource (origin.getFileName ().toString (), null);
	    if (fqn == null) System.err.println ("tn: " + tn);
	    cw.visit (V1_8, saf.flags, fqn, /*signature*/null, saf.supername, /*interfaces */null);
	}

	public SuperAndFlags getSuperAndFlags () {
	    String supername = "java/lang/Object";
	    int flags = 0;
	    if (tn instanceof NormalClassDeclaration) {
		NormalClassDeclaration ncd = (NormalClassDeclaration)tn;
		ClassType ct = ncd.getSuperClass ();
		if (ct != null) {
		    supername = ct.getFullName ().replace ('.', '/');
		}
		flags = ncd.getFlags ();
	    } else if (tn instanceof EnumDeclaration) {
		EnumDeclaration ed = (EnumDeclaration)tn;
		supername = "java.lang.Enum<" + ed.getId () + ">";
		flags = (ed.getFlags () | ACC_FINAL);
	    } else if (tn instanceof NormalInterfaceDeclaration) {
		NormalInterfaceDeclaration i = (NormalInterfaceDeclaration)tn;
		flags = i.getFlags () | ACC_INTERFACE;
	    }
	    // TODO: handle interface flags
	    return new SuperAndFlags (supername, flags);
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
	    String cid = cip.getFilename (tn) + ".class";
	    if (packageName == null)
		return Paths.get (cid);
	    return Paths.get (packageName.getPathName (), cid);
	}
    }

    private static class SuperAndFlags {
	public final String supername;
	public final int flags;
	public SuperAndFlags (String supername, int flags) {
	    this.supername = supername;
	    this.flags = flags;
	}
    }

    private static class MethodInfo {
	public final Result result;
	public final MethodVisitor mv;

	public MethodInfo (Result result, MethodVisitor mv) {
	    this.result = result;
	    this.mv = mv;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{result: " + result + ", mv: " + mv + "}";
	}
    }
}
