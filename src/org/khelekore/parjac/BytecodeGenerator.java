package org.khelekore.parjac;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.semantics.ClassInformationProvider;
import org.khelekore.parjac.semantics.FlagsHelper;
import org.khelekore.parjac.tree.*;
import org.khelekore.parjac.tree.Result.TypeResult;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeGenerator implements TreeVisitor {
    private final Path origin;
    private final ClassInformationProvider cip;
    private final BytecodeWriter classWriter;
    private DottedName packageName;
    private final Deque<ClassWriterHolder> classes = new ArrayDeque<> ();
    private ClassWriterHolder currentClass;
    private final Deque<MethodInfo> methods = new ArrayDeque<> ();
    private MethodInfo currentMethod;

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
	pushClass (c, c.getId ());
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	pushClass (e, e.getId ());
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	pushClass (i, i.getId ());
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	pushClass (a, a.getId ());
	return true;
    }

    @Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	pushClass (b, cip.getFilename (b));
	return true;
    }

    @Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	endType ();
    }

    private void pushClass (TreeNode tn, String name) {
	ClassWriterHolder cid = new ClassWriterHolder (tn, name);
	addClass (cid);
	cid.start ();
    }

    @Override public void endType () {
	ClassWriterHolder cid = removeClass ();
	cid.write ();
    }

    @Override public boolean visit (ConstructorDeclaration c) {
	ClassWriter cw = currentClass.cw;

	int mods = c.getFlags ();
	if (hasVarargs (c.getParameters ()))
	    mods |= ACC_VARARGS;

	MethodInfo mi = new MethodInfo (mods, new Result.VoidResult (null),
					cw.visitMethod (mods, "<init>", c.getDescription (), null, null));
	addMethod (mi);
	return true;
    }

    @Override public void endConstructor (ConstructorDeclaration c) {
	MethodInfo mi = removeMethod ();
	mi.mv.visitInsn (RETURN);
	mi.mv.visitMaxs (0, 0);
        mi.mv.visitEnd ();
    }

    @Override public boolean visit (ConstructorBody cb) {
	currentMethod.mv.visitVarInsn (ALOAD, 0); // pushes "this"
	ExplicitConstructorInvocation eci = cb.getConstructorInvocation ();
	if (eci == null) {
	    currentMethod.mv.visitMethodInsn (INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
	}
	return true;
    }

    @Override public void visit (ExplicitConstructorInvocation eci) {
	SuperAndFlags saf = currentClass.getSuperAndFlags ();
	// TODO: the signature here is borked, need to get signature from super class
	currentMethod.mv.visitMethodInsn (INVOKESPECIAL, saf.supername, "<init>", "()V", false);
    }

    @Override public boolean visit (FieldDeclaration f) {
	ClassWriter cw = currentClass.cw;
	int mods = f.getFlags ();
	if (FlagsHelper.isStatic (mods))
	    currentMethod = getStaticBlock ();
	// TODO: handle initializer blocks
	for (VariableDeclarator vd : f.getVariables ().get ()) {
	    String id = vd.getId ();
	    // int access, String name, String desc, String signature, Object value
	    FieldVisitor fw = cw.visitField (mods, id, getType (f.getType ()), null, null);
	    fw.visitEnd ();
	    TreeNode tn = vd.getInitializer ();
	    if (tn != null) {
		toType (tn, f).visit (this);
		storeValue (mods, id, f.getExpressionType ().getDescriptor ());
	    }
	}
	return false;
    }

    private void storeValue (int flags, String id, String type) {
	int op = PUTFIELD;
	if (FlagsHelper.isStatic (flags))
	    op = PUTSTATIC;
	String owner = currentClass.className;
	currentMethod.mv.visitFieldInsn (op, owner, id, type);
    }

    @Override public boolean visit (MethodDeclaration m) {
	ClassWriter cw = currentClass.cw;
        // creates a MethodWriter for the method
	int mods = m.getFlags ();
	if (hasVarargs (m.getParameters ()))
	    mods |= ACC_VARARGS;
        MethodInfo mi = new MethodInfo (mods, m.getResult (),
					cw.visitMethod (mods, m.getMethodName (),
							m.getDescription (), null, null));
	addMethod (mi);
	FormalParameterList ls = m.getParameters ();
	if (ls != null) {
	    NormalFormalParameterList fpl = m.getParameters ().getParameters ();
	    for (FormalParameter fp : fpl.getFormalParameters ())
		currentMethod.localVariableIds.put (fp.getId (), currentMethod.getId (fp));
	    LastFormalParameter lfp = fpl.getLastFormalParameter ();
	    if (lfp != null)
		currentMethod.localVariableIds.put (lfp.getId (), currentMethod.getId (lfp));
	}
	return true;
    }

    @Override public void endMethod (MethodDeclaration m) {
	MethodInfo mi = removeMethod ();
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

    private String getType (TreeNode tn) {
	StringBuilder sb = new StringBuilder ();
	appendType (tn, sb);
	return sb.toString ();
    }

    private void appendType (TreeNode tn, StringBuilder sb) {
	if (tn instanceof PrimitiveTokenType || tn instanceof ClassType) {
	    sb.append (tn.getExpressionType ().getDescriptor ());
	} else {
	    UnannArrayType at = (UnannArrayType)tn;
	    for (int i = 0, s = at.getDims ().get ().size (); i < s; i++)
		sb.append ("[");
	    appendType (at.getType (), sb);
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
	if (r.hasExpression ()) {
	    Result.TypeResult tr = (TypeResult)currentMethod.result;
	    ExpressionType et = tr.getReturnType ().getExpressionType ();
	    ExpressionType actual = r.getExpression ().getExpressionType ();
	    if (et.isPrimitiveType ()) {
		outputPrimitiveCasts (actual, et);
		if (et == ExpressionType.BOOLEAN ||
		    et == ExpressionType.BYTE ||
		    et == ExpressionType.SHORT ||
		    et == ExpressionType.CHAR ||
		    et == ExpressionType.INT) {
		    currentMethod.mv.visitInsn (IRETURN);
		} else if (et == ExpressionType.LONG) {
		    currentMethod.mv.visitInsn (LRETURN);
		} else if (et == ExpressionType.DOUBLE) {
		    currentMethod.mv.visitInsn (DRETURN);
		} else if (et == ExpressionType.FLOAT) {
		    currentMethod.mv.visitInsn (FRETURN);
		} else {
		    throw new IllegalArgumentException ("Unhandled type: " + et);
		}
	    } else {
		currentMethod.mv.visitInsn (ARETURN);
	    }
	} else {
	    currentMethod.mv.visitInsn (RETURN);
	}
    }

    @Override public void visit (IntLiteral iv) {
	int i = iv.get ();
	if (i >= -1 && i <= 5) {
	    currentMethod.mv.visitInsn (ICONST_M1 + 1 + i);
	} else if (i >= -128 && i <= 127) {
	    currentMethod.mv.visitIntInsn (BIPUSH, i);
	} else if (i >= -32768 && i <= 32767) {
	    currentMethod.mv.visitIntInsn (SIPUSH, i);
	} else {
	    currentMethod.mv.visitLdcInsn (i);
	}
    }

    @Override public void visit (LongLiteral lv) {
	long l = lv.get ();
	if (l == 0) {
	    currentMethod.mv.visitInsn (LCONST_0);
	} else if (l == 1) {
	    currentMethod.mv.visitInsn (LCONST_1);
	} else {
	    currentMethod.mv.visitLdcInsn (l);
	}
    }

    @Override public void visit (DoubleLiteral dv) {
	double d = dv.get ();
	if (d == 0) {
	    currentMethod.mv.visitInsn (DCONST_0);
	} else if (d == 1.0) {
	    currentMethod.mv.visitInsn (DCONST_1);
	} else {
	    currentMethod.mv.visitLdcInsn (d);
	}
    }

    @Override public void visit (FloatLiteral fv) {
	float f = fv.get ();
	if (f == 0) {
	    currentMethod.mv.visitInsn (FCONST_0);
	} else if (f == 1.0f) {
	    currentMethod.mv.visitInsn (FCONST_1);
	} else if (f == 2.0f) {
	    currentMethod.mv.visitInsn (FCONST_2);
	} else {
	    currentMethod.mv.visitLdcInsn (f);
	}
    }

    @Override public void visit (StringLiteral sv) {
	currentMethod.mv.visitLdcInsn (sv.get ());
    }

    @Override public void visit (BooleanLiteral bv) {
	currentMethod.mv.visitInsn (bv.get () ? ICONST_1 : ICONST_0);
    }

    @Override public void visit (NullLiteral nv) {
	currentMethod.mv.visitInsn (ACONST_NULL);
    }

    @Override public boolean visit (Assignment a) {
	// We do not handle init blocks yet.
	if (methods.isEmpty ())
	    return false;
	Token t = a.getOperator ().get ();
	if (t != Token.EQUAL)
	    a.lhs ().visit (this);
	TreeNode rhs = a.rhs ();
	if (!isShiftAssignment (t))
	    rhs = toType (a.rhs (), a);
	rhs.visit (this);
	int op = getAssignmentActionOp (t, a);
	if (op != -1)
	    currentMethod.mv.visitInsn (op);
	// TODO: this is pretty ugly, clean it up
	Identifier i = (Identifier)(((DottedName)a.lhs ()).getFieldAccess ());
	int id = currentMethod.localVariableIds.get (i.get ());
	storeValue (id, a.lhs ());
	return false;
    }

    private boolean isShiftAssignment (Token t) {
	return t == Token.LEFT_SHIFT_EQUAL ||
	    t == Token.RIGHT_SHIFT_EQUAL ||
	    t == Token.RIGHT_SHIFT_UNSIGNED_EQUAL;
    }

    private int getAssignmentActionOp (Token t, Assignment a) {
	ExpressionType et = a.getExpressionType ();
	if (et == ExpressionType.INT) {
	    switch (t) {
		//*=  /=  %=  +=  -=  <<=  >>=  >>>=  &=  ^=  |=
	    case MULTIPLY_EQUAL: return IMUL;
	    case DIVIDE_EQUAL: return IDIV;
	    case REMAINDER_EQUAL: return IREM;
	    case PLUS_EQUAL: return IADD;
	    case MINUS_EQUAL: return ISUB;
	    case LEFT_SHIFT_EQUAL: return ISHL;
	    case RIGHT_SHIFT_EQUAL: return ISHR;
	    case RIGHT_SHIFT_UNSIGNED_EQUAL: return IUSHR;
	    case BIT_AND_EQUAL: return IAND;
	    case BIT_XOR_EQUAL: return IXOR;
	    case BIT_OR_EQUAL: return IOR;
	    default:
	    }
	} else if (et == ExpressionType.LONG) {
	    switch (t) {
		//*=  /=  %=  +=  -=  <<=  >>=  >>>=  &=  ^=  |=
	    case MULTIPLY_EQUAL: return LMUL;
	    case DIVIDE_EQUAL: return LDIV;
	    case REMAINDER_EQUAL: return LREM;
	    case PLUS_EQUAL: return LADD;
	    case MINUS_EQUAL: return LSUB;
	    case LEFT_SHIFT_EQUAL: return LSHL;
	    case RIGHT_SHIFT_EQUAL: return LSHR;
	    case RIGHT_SHIFT_UNSIGNED_EQUAL: return LUSHR;
	    case BIT_AND_EQUAL: return LAND;
	    case BIT_XOR_EQUAL: return LXOR;
	    case BIT_OR_EQUAL: return LOR;
	    default:
	    }
	} else if (et == ExpressionType.FLOAT) {
	    switch (t) {
	    case PLUS_EQUAL: return FADD;
	    case MINUS_EQUAL: return FSUB;
	    case MULTIPLY_EQUAL: return FMUL;
	    case DIVIDE_EQUAL: return FDIV;
	    case REMAINDER_EQUAL: return FREM;
	    default:
		// nothing
	    }
	} else if (et == ExpressionType.DOUBLE) {
	    switch (t) {
	    case PLUS_EQUAL: return DADD;
	    case MINUS_EQUAL: return DSUB;
	    case MULTIPLY_EQUAL: return DMUL;
	    case DIVIDE_EQUAL: return DDIV;
	    case REMAINDER_EQUAL: return DREM;
	    default:
		// nothing
	    }
	}
	return -1;
    }

    @Override public boolean visit (ClassInstanceCreationExpression c) {
	String sname = c.getId ().getSlashName ();
	currentMethod.mv.visitTypeInsn (NEW, sname);
	currentMethod.mv.visitInsn (DUP);
	ArgumentList ls = c.getArgumentList ();
	if (ls != null)
	    c.getArgumentList ().visit (this);
	currentMethod.mv.visitMethodInsn (INVOKESPECIAL, sname, "<init>", c.getDescription (), false);
	return false;
    }

    @Override public boolean visit (MethodInvocation m) {
	String owner;
	TreeNode on = m.getOn ();
	String ownerName = null;
	if (on != null) {
	    on.visit (this);
	    ExpressionType expType = on.getExpressionType ();
	    owner = expType.getSlashName ();
	    ownerName = expType.getClassName ();
	} else {
	    ownerName = cip.getFullName (classes.peekLast ().tn);
	    owner = ownerName.replace ('.', '/');
	}
	ArgumentList al = m.getArgumentList ();
	if (al != null) {
	    al.visit (this);
	}
	int opCode = INVOKEVIRTUAL;
	boolean isInterface = cip.isInterface (ownerName);
	if (FlagsHelper.isStatic (m.getActualMethodFlags ()))
	    opCode = INVOKESTATIC;
	else if (isInterface)
	    opCode = INVOKEINTERFACE;
	currentMethod.mv.visitMethodInsn (opCode, owner, m.getId (), m.getDescription (), isInterface);
	return false;
    }

    @Override public boolean visit (IfThenStatement i) {
	i.getExpression ().visit (this);
	Label elseStart = new Label ();
	TreeNode exp = i.getExpression ();
	handleJump (exp, elseStart);
	i.getIfStatement ().visit (this);
	currentMethod.mv.visitLabel (elseStart);
	TreeNode tn = i.getElseStatement ();
	if (tn != null)
	    tn.visit (this);
	return false;
    }

    @Override public boolean visit (WhileStatement w) {
	Label start = new Label ();
	Label end = new Label ();
	currentMethod.mv.visitLabel (start);
	TreeNode tn = w.getExpression ();
	tn.visit (this);
	handleJump (tn, end);
	tn = w.getStatement ();
	if (tn != null)
	    tn.visit (this);
	currentMethod.mv.visitJumpInsn (GOTO, start);
	currentMethod.mv.visitLabel (end);
	return false;
    }

    @Override public boolean visit (DoStatement d) {
	Label start = new Label ();
	Label end = new Label ();
	currentMethod.mv.visitLabel (start);
	TreeNode tn = d.getStatement ();
	if (tn != null)
	    tn.visit (this);
	tn = d.getExpression ();
	tn.visit (this);
	handleJump (tn, end);
	currentMethod.mv.visitJumpInsn (GOTO, start);
	currentMethod.mv.visitLabel (end);
	return false;
    }

    @Override public boolean visit (BasicForStatement f) {
	TreeNode tn;
	if ((tn = f.getForInit()) != null)
	    tn.visit (this);
	Label start = new Label ();
	currentMethod.mv.visitLabel (start);
	Label end = new Label ();
	if ((tn = f.getExpression ()) != null) {
	    tn.visit (this);
	    handleJump (tn, end);
	}
	if ((tn = f.getStatement ()) != null)
	    tn.visit (this);
	if ((tn = f.getForUpdate ()) != null)
	    tn.visit (this);
	currentMethod.mv.visitJumpInsn (GOTO, start);
	currentMethod.mv.visitLabel (end);
	return false;
    }

    private void handleJump (TreeNode exp, Label target) {
	int operator = IFEQ;
	if (exp instanceof UnaryExpression) {
	    operator = IFNE;
	} else if (exp instanceof TwoPartExpression) {
	    operator = getTwoPartJump ((TwoPartExpression)exp);
	}
	currentMethod.mv.visitJumpInsn (operator, target);
    }

    private int getTwoPartJump (TwoPartExpression t) {
	// Remember that we jump to the else part so reversed return values
	if (t.hasPrimitiveParts ()) {
	    switch (t.getOperator ()) {
	    case DOUBLE_EQUAL: return IF_ICMPNE;
	    case NOT_EQUAL: return IF_ICMPEQ;
	    case LT: return IF_ICMPGE;
	    case GE: return IF_ICMPLT;
	    case GT: return IF_ICMPLE;
	    case LE: return IF_ICMPGT;
	    default:
		throw new IllegalStateException ("unhandled jump type: " + t);
	    }
	} else {
	    switch (t.getOperator ()) {
	    case DOUBLE_EQUAL: return IF_ACMPNE;
	    case NOT_EQUAL: return IF_ACMPEQ;
	    default:
		throw new IllegalStateException ("unhandled jump type: " + t);
	    }
	}
    }

    @Override public boolean visit (LocalVariableDeclaration l) {
	for (VariableDeclarator vd : l.getVariables ().get ()) {
	    int id = currentMethod.getId (l);
	    currentMethod.localVariableIds.put (vd.getId (), id);
	    TreeNode tn = vd.getInitializer ();
	    if (tn != null) {
		toType (tn, l).visit (this);
		storeValue (id, l);
	    }
	}
	return false;
    }

    private void storeValue (int id, TreeNode tn) {
	int op = ISTORE;
	ExpressionType et = tn.getExpressionType ();
	if (!et.isPrimitiveType ()) {
	    op = ASTORE;
	}else if (et == ExpressionType.LONG) {
	    op = LSTORE;
	} else if (et == ExpressionType.FLOAT) {
	    op = FSTORE;
	} else  if (et == ExpressionType.DOUBLE) {
	    op = DSTORE;
	}
	currentMethod.mv.visitVarInsn (op, id);
    }

    @Override public boolean visit (FieldAccess f) {
	TreeNode tn = f.getFrom ();
	if (tn instanceof ClassType) {
	    ClassType ct = (ClassType)tn;
	    String classSignature = ct.getFullName ().replace ('.', '/');
	    currentMethod.mv.visitFieldInsn (GETSTATIC, classSignature, f.getFieldId (),
					     f.getExpressionType ().getDescriptor ());
	    return false;
	} else {
	    return true;
	}
    }

    @Override public boolean visit (CastExpression c) {
	c.getExpression ().visit (this);
	ExpressionType target = c.getExpressionType ();
	if (target.isPrimitiveType ()) {
	    ExpressionType source = c.getExpression ().getExpressionType ();
	    outputPrimitiveCasts (source, target);
	} else {
	    currentMethod.mv.visitTypeInsn (CHECKCAST, target.getSlashName ());
	}
	return false;
    }

    private void outputPrimitiveCasts (ExpressionType source, ExpressionType target) {
	if (source == target)
	    return;
	if (target == ExpressionType.INT && isIntType (source))
	    return;
	if (requireIntConversion (source, target)) {
	    currentMethod.mv.visitInsn (getCast (source, ExpressionType.INT));
	    source = ExpressionType.INT;
	}
	currentMethod.mv.visitInsn (getCast (source, target));
    }

    private boolean requireIntConversion (ExpressionType source, ExpressionType target) {
	if (source == ExpressionType.LONG ||
	    source == ExpressionType.FLOAT ||
	    source == ExpressionType.DOUBLE) {
	    return target == ExpressionType.BYTE ||
		target == ExpressionType.CHAR ||
		target == ExpressionType.SHORT;
	}
	return false;
    }

    private int getCast (ExpressionType source, ExpressionType target) {
	if (target == ExpressionType.BYTE) {
	    return I2B;
	} else if (target == ExpressionType.CHAR) {
	    return I2C;
	} else if (target == ExpressionType.SHORT) {
	    return I2S;
	} else if (target == ExpressionType.INT) {
	    if (source == ExpressionType.LONG) {
		return L2I;
	    } else if (source == ExpressionType.FLOAT) {
		return F2I;
	    } else if (source == ExpressionType.DOUBLE) {
		return D2I;
	    }
	} else if (target == ExpressionType.LONG) {
	    if (isIntType (source)) {
		return I2L;
	    } else if (source == ExpressionType.FLOAT) {
		return F2L;
	    } else if (source == ExpressionType.DOUBLE) {
		return D2L;
	    }
	} else if (target == ExpressionType.FLOAT) {
	    if (isIntType (source)) {
		return I2F;
	    } else if (source == ExpressionType.LONG) {
		return L2F;
	    } else if (source == ExpressionType.DOUBLE) {
		return D2F;
	    }
	} else if (target == ExpressionType.DOUBLE) {
	    if (isIntType (source)) {
		return I2D;
	    } else if (source == ExpressionType.LONG) {
		return L2D;
	    } else if (source == ExpressionType.FLOAT) {
		return F2D;
	    }
	}
	throw new IllegalStateException ("Unable to cast from: " + source + " to "  + target);
    }

    private boolean isIntType (ExpressionType et) {
	return et == ExpressionType.BYTE ||
	    et == ExpressionType.CHAR ||
	    et == ExpressionType.SHORT ||
	    et == ExpressionType.INT;
    }

    @Override public void visit (Identifier i) {
	Integer lid = currentMethod.localVariableIds.get (i.get ());
	if (lid == null)
	    lid = 0;
	ExpressionType et = i.getExpressionType ();
	int op = ILOAD;
	if (!et.isPrimitiveType ()) {
	    op = ALOAD;
	}else if (et == ExpressionType.LONG) {
	    op = LLOAD;
	} else if (et == ExpressionType.FLOAT) {
	    op = FLOAD;
	} else  if (et == ExpressionType.DOUBLE) {
	    op = DLOAD;
	}
	currentMethod.mv.visitVarInsn (op, lid);
    }

    @Override public boolean visit (TwoPartExpression t) {
	TreeNode left = t.getLeft ();
	TreeNode right = t.getRight ();
	if (t.getOperator ().isAutoWidenOperator ()) {
	    left = toType (left, t);
	    right = toType (right, t);
	}
	left.visit (this);
	right.visit (this);
	int op = getArithmeticOperation (t);
	if (op != -1)
	    currentMethod.mv.visitInsn (op);
	return false;
    }

    private TreeNode toType (TreeNode tn, TreeNode wantedType) {
	ExpressionType et = wantedType.getExpressionType ();
	ExpressionType current = tn.getExpressionType ();
	if (!et.equals (current)) {
	    if (tn instanceof LiteralValue) {
		if (current == ExpressionType.INT) {
		    if (et == ExpressionType.LONG)
			return new LongLiteral (((IntLiteral)tn).get (), tn.getParsePosition ());
		    else if (et == ExpressionType.FLOAT)
			return new FloatLiteral (((IntLiteral)tn).get (), tn.getParsePosition ());
		    else if (et == ExpressionType.DOUBLE)
			return new DoubleLiteral (((IntLiteral)tn).get (), tn.getParsePosition ());
		} else if (current == ExpressionType.FLOAT) {
		    if (et == ExpressionType.DOUBLE)
			return new DoubleLiteral (((FloatLiteral)tn).get (), tn.getParsePosition ());
		}
	    } else {
		return new CastExpression (wantedType, tn, tn.getParsePosition ());
	    }
	}
	return tn;
    }

    private int getArithmeticOperation (TwoPartExpression t) {
	ExpressionType et = t.getExpressionType ();
	if (et == ExpressionType.INT) {
	    switch (t.getOperator ()) {
	    case PLUS: return IADD;
	    case MINUS: return ISUB;
	    case MULTIPLY: return IMUL;
	    case DIVIDE: return IDIV;
	    case REMAINDER: return IREM;
	    case LEFT_SHIFT: return ISHL;
	    case RIGHT_SHIFT: return ISHR;
	    case RIGHT_SHIFT_UNSIGNED: return IUSHR;
	    case AND: return IAND;
	    case OR: return IOR;
	    case XOR: return IXOR;
	    default:
		// nothing
	    }
	} else if (et == ExpressionType.LONG) {
	    switch (t.getOperator ()) {
	    case PLUS: return LADD;
	    case MINUS: return LSUB;
	    case MULTIPLY: return LMUL;
	    case DIVIDE: return LDIV;
	    case REMAINDER: return LREM;
	    case LEFT_SHIFT: return LSHL;
	    case RIGHT_SHIFT: return LSHR;
	    case RIGHT_SHIFT_UNSIGNED: return LUSHR;
	    case AND: return LAND;
	    case OR: return LOR;
	    case XOR: return LXOR;
	    default:
		// nothing
	    }
	} else if (et == ExpressionType.FLOAT) {
	    switch (t.getOperator ()) {
	    case PLUS: return FADD;
	    case MINUS: return FSUB;
	    case MULTIPLY: return FMUL;
	    case DIVIDE: return FDIV;
	    case REMAINDER: return FREM;
	    default:
		// nothing
	    }
	} else if (et == ExpressionType.DOUBLE) {
	    switch (t.getOperator ()) {
	    case PLUS: return DADD;
	    case MINUS: return DSUB;
	    case MULTIPLY: return DMUL;
	    case DIVIDE: return DDIV;
	    case REMAINDER: return DREM;
	    default:
		// nothing
	    }
	}
	return -1;
    }

    @Override public boolean visit (PreIncrementExpression p) {
	currentMethod.mv.visitIincInsn (getId (p.getExp ()), 1);
	return false;
    }

    @Override public boolean visit (PostIncrementExpression p) {
	currentMethod.mv.visitIincInsn (getId (p.getExp ()), 1);
	return false;
    }

    @Override public boolean visit (PreDecrementExpression p) {
	currentMethod.mv.visitIincInsn (getId (p.getExp ()), -1);
	return false;
    }

    @Override public boolean visit (PostDecrementExpression p) {
	currentMethod.mv.visitIincInsn (getId (p.getExp ()), -1);
	return false;
    }

    private int getId (TreeNode tn) {
	if (tn instanceof DottedName)
	    tn = ((DottedName)tn).getFieldAccess ();
	if (tn instanceof Identifier) {
	    Identifier i = (Identifier)tn;
	    return currentMethod.localVariableIds.get (i.get ());
	}
	return 1; // TODO: not correct, but works for now
    }

    private void addClass (ClassWriterHolder cw) {
	classes.addLast (cw);
	currentClass = cw;
    }

    private ClassWriterHolder removeClass () {
	ClassWriterHolder cw = classes.removeLast ();
	currentClass = classes.isEmpty () ? null : classes.peekLast ();
	return cw;
    }

    private void addMethod (MethodInfo mi) {
	methods.addLast (mi);
	currentMethod = mi;
    }

    private MethodInfo removeMethod () {
	MethodInfo ret = methods.removeLast ();
	currentMethod = methods.isEmpty () ? null : methods.peekLast ();
	return ret;
    }

    private MethodInfo getStaticBlock () {
	MethodInfo sb = currentClass.staticBlock;
	if (sb == null) {
	    int mods = Opcodes.ACC_STATIC;
	    ClassWriter cw = currentClass.cw;
	    sb = currentClass.staticBlock =
		new MethodInfo (mods, new Result.VoidResult (null),
				cw.visitMethod (mods, "<clinit>", "()V", null, null));
	}
	return sb;
    }

    private class ClassWriterHolder {
	private final TreeNode tn;
	private final String className;
	private final ClassWriter cw;
	private MethodInfo staticBlock;

	public ClassWriterHolder (TreeNode tn, String className) {
	    this.tn = tn;
	    this.className = className;
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
	    endStaticBlock ();
	    Path path = getPath ();
	    try {
		classWriter.write (path, cw.toByteArray ());
	    } catch (IOException e) {
		System.err.println ("Failed to create class file: " + path);
	    }
	}

	private void endStaticBlock () {
	    MethodInfo mi = staticBlock;
	    if (mi == null)
		return;
	    mi.mv.visitInsn (RETURN);
	    mi.mv.visitMaxs (0, 0);
	    mi.mv.visitEnd ();
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
	public final Map<String, Integer> localVariableIds = new HashMap<> ();
	private int nextId;

	public MethodInfo (int flags, Result result, MethodVisitor mv) {
	    nextId = FlagsHelper.isStatic (flags) ? 0 : 1;
	    this.result = result;
	    this.mv = mv;
	}

	public int getId (TreeNode tn) {
	    ExpressionType et = tn.getExpressionType ();
	    int ret = nextId;
	    if (et == ExpressionType.LONG || et == ExpressionType.DOUBLE)
		nextId++;
	    nextId++;
	    return ret;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{result: " + result + ", mv: " + mv + "}";
	}
    }
}
