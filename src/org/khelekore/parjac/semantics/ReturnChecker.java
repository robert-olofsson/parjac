package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.Assignment;
import org.khelekore.parjac.tree.BasicForStatement;
import org.khelekore.parjac.tree.Block;
import org.khelekore.parjac.tree.BlockStatements;
import org.khelekore.parjac.tree.BooleanLiteral;
import org.khelekore.parjac.tree.BreakStatement;
import org.khelekore.parjac.tree.CastExpression;
import org.khelekore.parjac.tree.Catches;
import org.khelekore.parjac.tree.ContinueStatement;
import org.khelekore.parjac.tree.DoStatement;
import org.khelekore.parjac.tree.DottedName;
import org.khelekore.parjac.tree.EnhancedForStatement;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.Finally;
import org.khelekore.parjac.tree.Identifier;
import org.khelekore.parjac.tree.IfThenStatement;
import org.khelekore.parjac.tree.IntLiteral;
import org.khelekore.parjac.tree.LabeledStatement;
import org.khelekore.parjac.tree.LocalVariableDeclaration;
import org.khelekore.parjac.tree.MethodBody;
import org.khelekore.parjac.tree.MethodDeclaration;
import org.khelekore.parjac.tree.PostDecrementExpression;
import org.khelekore.parjac.tree.PostIncrementExpression;
import org.khelekore.parjac.tree.PreDecrementExpression;
import org.khelekore.parjac.tree.PreIncrementExpression;
import org.khelekore.parjac.tree.Result;
import org.khelekore.parjac.tree.ReturnStatement;
import org.khelekore.parjac.tree.SameTypeTreeVisitor;
import org.khelekore.parjac.tree.SwitchBlock;
import org.khelekore.parjac.tree.SwitchBlockStatementGroup;
import org.khelekore.parjac.tree.SwitchLabel;
import org.khelekore.parjac.tree.SwitchStatement;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TernaryExpression;
import org.khelekore.parjac.tree.ThrowStatement;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;
import org.khelekore.parjac.tree.TryStatement;
import org.khelekore.parjac.tree.TwoPartExpression;
import org.khelekore.parjac.tree.UnaryExpression;
import org.khelekore.parjac.tree.VariableDeclarator;
import org.khelekore.parjac.tree.WhileStatement;

/** Test that we return correct types from methods and if and while checks as
 *  well as two part things.
 */
public class ReturnChecker implements TreeVisitor {
    private final ClassInformationProvider cip;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final Deque<MethodData> methods = new ArrayDeque<> ();

    public ReturnChecker (ClassInformationProvider cip, SyntaxTree tree,
			  CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void run () {
	tree.getCompilationUnit ().visit (this);
    }

    @Override public boolean visit (MethodDeclaration md) {
	methods.push (new MethodData ());
	MethodBody b = md.getBody ();
	if (!b.isEmpty ()) {
	    if (!checkMethodBlock (b.getBlock (), md.getResult ()))
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     b.getParsePosition (),
							     "Method missing return"));
	}
	return true;
    }

    @Override public void endMethod (MethodDeclaration m) {
	methods.pop ();
    }

    private boolean checkMethodBlock (Block b, Result res) {
	Ender ends = checkStatements (b.getStatements (), res);
	return ends.ended () || res instanceof Result.VoidResult;
    }

    private Ender checkBlock (Block b, Result res) {
	return checkStatements (b.getStatements (), res);
    }

    private Ender checkStatements (List<TreeNode> statements, Result res) {
	if (statements == null)
	    return new Ender ();
	Ender ender = null;
	StatementChecker sc = new StatementChecker (res);
	for (int i = 0, s = statements.size (); i < s; i++) {
	    TreeNode tn = statements.get (i);
	    tn.visit (sc);
	    ender = sc.ender;
	    if (ender.ended () && i < s -1) {
		TreeNode next = statements.get (i + 1);
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     next.getParsePosition (),
							     "Unreachable code"));
	    }
	}
	if (ender == null)
	    ender = new Ender ();
	return ender;
    }

    private class StatementChecker extends SameTypeTreeVisitor {
	private final Result res;
	private Ender ender = new Ender ();

	public StatementChecker (Result res) {
	    this.res = res;
	}

	@Override public boolean visit (ReturnStatement r) {
	    ender.hasReturns = true;
	    checkType (r, res);
	    return true;
	}

	@Override public boolean visit (Assignment a) {
	    TreeNode left = a.lhs ();
	    TreeNode right = a.rhs ();
	    right.visit (this);
	    if (!match (left, right)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     a.getParsePosition (),
							     "Can not set: %s to: %s",
							     left.getExpressionType (),
							     right.getExpressionType ()));
	    }
	    setInitialized (left);
	    return false;
	}

	private void setInitialized (TreeNode tn) {
	    if (tn instanceof DottedName) {
		DottedName dn = (DottedName)tn;
		TreeNode fa = dn.getFieldAccess ();
		if (fa instanceof Identifier) {
		    Identifier i = (Identifier)fa;
		    methods.peek ().uninitializedLocals.remove (i.get ());
		}
	    }
	}

	@Override public boolean visit (TernaryExpression t) {
	    checkBoolean (t.getExpression ());
	    Set<String> uninited = new HashSet<> (methods.peek ().uninitializedLocals);
	    t.getThenPart ().visit (this);
	    Set<String> uninited2 = new HashSet<> (methods.peek ().uninitializedLocals);
	    methods.peek ().uninitializedLocals = uninited;
	    t.getElsePart ().visit (this);
	    methods.peek ().uninitializedLocals.addAll (uninited2);
	    return false;
	}

	@Override public void visit (LabeledStatement l) {
	    TreeNode s = l.getStatement ();
	    if (!allowedLabelTarget (s))
		diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (),
							       l.getParsePosition (),
							       "Unusable label"));
	    if (!methods.peek ().activeLabels.add (l.getId ()))
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     l.getParsePosition (),
							     "Label %s already in use",
							     l.getId ()));
	}

	@Override public void endLabel (LabeledStatement l) {
	    String id = l.getId ();
	    methods.peek ().activeLabels.remove (id);
	    ender.hasBreak.remove (id);
	    ender.mayBreak.remove (id);
	}

	private boolean allowedLabelTarget (TreeNode t) {
	    return t instanceof BasicForStatement ||
		t instanceof EnhancedForStatement ||
		t instanceof WhileStatement ||
		t instanceof DoStatement ||
		t instanceof SwitchStatement; // only for break
	}

	@Override public void visit (ThrowStatement t) {
	    ender.hasThrow = true;
	    ender.mayThrow = true;
	}

	@Override public void visit (BreakStatement b) {
	    String id = b.getId ();
	    checkValidLabel (b, id);
	    if (id == null)
		id = "";
	    ender.hasBreak.add (id);
	    ender.mayBreak.add (id);
	}

	@Override public void visit (ContinueStatement b) {
	    checkValidLabel (b, b.getId ());
	}

	private void checkValidLabel (TreeNode t, String id) {
	    if (id != null && !methods.peek ().activeLabels.contains (id))
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     t.getParsePosition (),
							     "No label: %s in scope",
							     id));
	}

	@Override public boolean visit (IfThenStatement i) {
	    checkBoolean (i.getExpression ());
	    Set<String> uninited = new HashSet<> (methods.peek ().uninitializedLocals);
	    Ender statementEnds = checkStatement (i.getIfStatement ());
	    Ender elseEnds = null;
	    TreeNode elseStatement = i.getElseStatement ();
	    if (elseStatement == null) {
		if (isTrue (i.getExpression ()))
		    ender = statementEnds;
		else
		    ender.mayRun ();
		methods.peek ().uninitializedLocals = uninited;
	    } else {
		Set<String> uninited2 = new HashSet<> (methods.peek ().uninitializedLocals);
		methods.peek ().uninitializedLocals = uninited;
		elseEnds = checkStatement (elseStatement);
		statementEnds.and (elseEnds);
		ender = statementEnds;
		methods.peek ().uninitializedLocals.addAll (uninited2);
	    }
	    return false;
	}

	@Override public boolean visit (WhileStatement w) {
	    checkBoolean (w.getExpression ());
	    ender = checkExpressionStatement (w.getExpression (), w.getStatement ());
	    ender.isInfiniteLoop &= ender.mayBreak.isEmpty ();
	    ender.hasBreak.remove (""); // clear for outer levels
	    ender.mayBreak.remove (""); // clear for outer levels
	    if (!isTrue (w.getExpression ()))
		ender.mayRun ();
	    return false;
	}

	@Override public boolean visit (DoStatement d) {
	    checkBoolean (d.getExpression ());
	    ender = checkExpressionStatement (d.getExpression (), d.getStatement ());
	    return false;
	}

	@Override public boolean visit (BasicForStatement f) {
	    TreeNode exp = f.getExpression ();
	    ender = checkExpressionStatement (exp, f.getStatement ());
	    if (exp == null)
		ender.isInfiniteLoop = true;
	    else
		ender.mayRun ();
	    ender.isInfiniteLoop &= ender.mayBreak.isEmpty ();
	    ender.hasBreak.remove (""); // clear for outer levels
	    ender.mayBreak.remove (""); // clear for outer levels
	    return false;
	}

	@Override public boolean visit (EnhancedForStatement f) {
	    checkStatement (f.getStatement ());
	    return false;
	}

	private Ender checkExpressionStatement (TreeNode exp, TreeNode statement) {
	    Ender statementEnder = checkStatement (statement);
	    if (isTrue (exp))
		statementEnder.isInfiniteLoop = true;
	    return statementEnder;
	}

	private boolean isTrue (TreeNode t) {
	    if (t instanceof BooleanLiteral)
		return ((BooleanLiteral)t).get ();
	    return false;
	}

	@Override public boolean visit (SwitchStatement s) {
	    SwitchBlock b = s.getBlock ();
	    /* default can be anywhere
	     * statements can return, throw or break to not fall through
	     */
	    Ender lastEnds = ender;
	    boolean hasDefault = false;
	    List<SwitchBlockStatementGroup> ls = b.getGroups ();
	    for (int i = 0, n = ls.size (); i < n; i++) {
		SwitchBlockStatementGroup sg = ls.get (i);
		lastEnds = checkStatement (sg.getStatements ());
		if (hasDefault (sg.getLabels ())) {
		    if (hasDefault)
			diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
								     sg.getParsePosition (),
								     "Duplicate default clause found"));
		    hasDefault = true;
		}
	    }
	    if (!hasDefault)
		lastEnds.reset ();
	    ender = lastEnds;
	    return false;
	}

	private Ender checkStatement (TreeNode t) {
	    if (t instanceof ReturnStatement)
		visit ((ReturnStatement)t);
	    else if (t instanceof Block)
		ender = checkBlock ((Block)t, res);
	    else if (t instanceof BlockStatements)
		ender = checkStatements (((BlockStatements)t).get (), res);
	    else
		t.visit (this);
	    return ender;
	}

	private boolean hasDefault (List<SwitchLabel> labels) {
	    return labels.stream ().anyMatch (l -> l instanceof SwitchLabel.DefaultLabel);
	}

	@Override public boolean visit (TryStatement t) {
	    Block block = t.getBlock ();
	    Ender blockEnds = checkBlock (block, res);
	    Ender finallyEnds = null;
	    Catches c = t.getCatches ();
	    if (c != null)
		c.get ().forEach (cc -> checkBlock (cc.getBlock (), res));
	    Finally f = t.getFinallyBlock ();
	    if (f != null)
		finallyEnds = checkBlock (f.getBlock (), res);
	    if (finallyEnds != null) {
		if (finallyEnds.hasReturns) {
		    diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (),
								   f.getParsePosition (),
								   "Return in finally"));
		}
		if (finallyEnds.hasThrow)
		    diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (),
								   f.getParsePosition (),
								   "Throw in finally"));
		blockEnds.merge (finallyEnds);
	    }
	    ender = blockEnds;
	    return false;
	}

	private void checkBoolean (TreeNode tn) {
	    if (tn.getExpressionType () != ExpressionType.BOOLEAN) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     tn.getParsePosition (),
							     "Not a boolean expression: %s, node: %s",
							     tn.getExpressionType (), tn));
	    }
	}

	@Override public boolean visit (LocalVariableDeclaration l) {
	    for (VariableDeclarator vd : l.getVariables ().get ()) {
		TreeNode initializer = vd.getInitializer ();
		if (initializer != null) {
		    if (!match (l.getType (), initializer)) {
			diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
								     initializer.getParsePosition (),
								     "Wrong type, require: %s, found: %s",
								     l.getExpressionType (),
								     initializer.getExpressionType ()));
		    }
		} else {
		    methods.peek ().uninitializedLocals.add (vd.getId ());
		}
	    }
	    return true;
	}

	@Override public boolean visit (CastExpression c) {
	    if (match (c.getType (), c.getExpression ())) {
		diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (),
							       c.getParsePosition (),
							       "Unneccessary cast from %s to %s",
							       c.getExpression ().getExpressionType (),
							       c.getType ().getExpressionType ()));
	    }
	    return true;
	}

	@Override public void visit (UnaryExpression u) {
	    Token op = u.getOperator ();
	    switch (op) {
	    case TILDE:
		checkIntegralType (u, op);
		break;
	    case NOT:
		checkBooleanType (u, op);
		break;
	    default:
	    }
	}

	@Override public boolean visit (TwoPartExpression t) {
	    Token op = t.getOperator ();
	    TreeNode lhs = t.getLeft ();
	    TreeNode rhs = t.getRight ();
	    if (lhs.getExpressionType () == ExpressionType.NULL &&
		op != Token.DOUBLE_EQUAL && op != Token.NOT_EQUAL) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     lhs.getParsePosition (),
							     "Can not use null on left hand side of: %s", op));
	    }
	    if (rhs.getExpressionType () == ExpressionType.NULL &&
		op != Token.DOUBLE_EQUAL && op != Token.NOT_EQUAL) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     rhs.getParsePosition (),
							     "Can not use null on right hand side of: %s", op));
	    }
	    if (op.isBitOrShiftOperator ()) {
		// TODO: need to check for wrapper types
		checkIntegralType(lhs, op);
		checkIntegralType(rhs, op);
	    }
	    return true;
	}

	@Override public boolean visit (PreIncrementExpression p) {
	    checkNumericType (p, Token.INCREMENT);
	    return true;
	}

	@Override public boolean visit (PostIncrementExpression p) {
	    checkNumericType (p, Token.INCREMENT);
	    return true;
	}

	@Override public boolean visit (PreDecrementExpression p) {
	    checkNumericType (p, Token.INCREMENT);
	    return true;
	}

	@Override public boolean visit (PostDecrementExpression p) {
	    checkNumericType (p, Token.INCREMENT);
	    return true;
	}

	private void checkIntegralType (TreeNode tn, Token operator) {
	    ExpressionType et = tn.getExpressionType ();
	    if (!et.isIntegralType ())
		reportWrongType (tn, operator, "integral", et);
	}

	private void checkBooleanType (TreeNode tn, Token operator) {
	    ExpressionType et = tn.getExpressionType ();
	    if (!et.isBooleanType ())
		reportWrongType (tn, operator, "boolean", et);
	}

	private void checkNumericType (TreeNode tn, Token operator) {
	    ExpressionType et = tn.getExpressionType ();
	    if (!et.isNumericType ())
		reportWrongType (tn, operator, "numeric", et);
	}

	private void reportWrongType (TreeNode tn, Token operator, String requiredType, ExpressionType et) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 tn.getParsePosition (),
							 "Operator: %s require %s type, found: %s",
							 operator, requiredType, et.toString ()));
	}
    }

    private void checkType (ReturnStatement rs, Result res) {
	if (res instanceof Result.VoidResult) {
	    if (rs.hasExpression ())
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     rs.getParsePosition (),
							     "Void method may not return value"));
	} else {
	    if (!rs.hasExpression ()) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     rs.getParsePosition (),
							     "Return value expected"));
		return;
	    }
	    TreeNode type = res.getReturnType ();
	    TreeNode exp = rs.getExpression ();
	    if (!match (type, exp)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     rs.getParsePosition (),
							     "Wrong return type: found: %s" +
							     ", expected: %s",
							     exp.getExpressionType (),
							     type.getExpressionType ()));
	    }
	    checkInitialized (exp);
	}
    }

    private void checkInitialized (TreeNode tn) {
	tn.visit (new TreeVisitor () {
	    @Override public void visit (Identifier i) {
		if (methods.peek ().uninitializedLocals.contains (i.get ())) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
								 i.getParsePosition (),
								 "Uninitialized variable"));
		}
	    }
	});
    }

    private boolean match (TreeNode result, TreeNode exp) {
	ExpressionType expType = exp.getExpressionType ();
	ExpressionType resultType = result.getExpressionType ();
	if (Objects.equals (expType, resultType))
	    return true;
	if (resultType == null || expType == null)
	    return false;
	if (resultType.isPrimitiveType () != expType.isPrimitiveType ())
	    return false;
	// Ok, not a direct match
	if (resultType.isPrimitiveType ()) {
	    // need to allow for implicit upcast
	    if (ExpressionType.mayBeAutoCasted (expType, resultType))
		return true;
	    if (exp instanceof IntLiteral) {
		IntLiteral il = (IntLiteral)exp;
		int v = il.get ();
		if (resultType == ExpressionType.BYTE) {
		    if (v < Byte.MAX_VALUE && v > Byte.MIN_VALUE)
			return true;
		} else if (resultType == ExpressionType.SHORT) {
		    if (v < Short.MAX_VALUE && v > Short.MIN_VALUE)
			return true;
		} else if (resultType == ExpressionType.CHAR) {
		    if (v < Character.MAX_VALUE && v > Character.MIN_VALUE)
			return true;
		}
	    }
	    return false;
	}
	if (expType == ExpressionType.NULL)
	    return true;
	return isSubType (expType, resultType);
    }

    private boolean isSubType (ExpressionType sub, ExpressionType sup) {
	try {
	    Optional<List<String>> supers = cip.getSuperTypes (sub.getClassName());
	    if (supers.isPresent ()) {
		List<String> l = supers.get ();
		for (String s : l) {
		    if (s.equals (sup.getClassName ()))
			return true;
		    if (isSubType (new ExpressionType (s), sup))
			return true;
		}
	    }
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to load super classes for %s", sub));
	}
	return false;
    }

    private static class Ender {
	private boolean hasReturns; // guaranteed to return
	private Set<String> hasBreak = new HashSet<> ();
	private Set<String> mayBreak = new HashSet<> ();
	private boolean hasThrow;
	private boolean mayThrow;
	private boolean isInfiniteLoop;

	public boolean ended () {
	    return hasReturns || hasThrow || !hasBreak.isEmpty () || (isInfiniteLoop && mayBreak.isEmpty ());
	}

	public Ender and (Ender other) {
	    Ender res = new Ender ();
	    res.hasReturns = hasReturns && other.hasReturns;
	    res.hasBreak = new HashSet<> (hasBreak);
	    res.hasBreak.retainAll (other.hasBreak);
	    res.mayBreak = new HashSet<> (mayBreak);
	    res.mayBreak.addAll (other.mayBreak);
	    res.hasThrow = hasThrow && other.hasThrow;
	    res.isInfiniteLoop = isInfiniteLoop && other.isInfiniteLoop;
	    return res;
	}

	public void merge (Ender other) {
	    isInfiniteLoop |= other.isInfiniteLoop;
	    if (other.isInfiniteLoop) {
		hasReturns |= other.hasReturns;
		hasBreak.addAll (other.hasBreak);
		hasThrow |= other.hasThrow;
	    }
	}

	public void reset () {
	    hasReturns = false;
	    hasBreak.clear ();
	    mayBreak.clear ();
	    hasThrow = false;
	    mayThrow = false;
	    isInfiniteLoop = false;
	}

	public void mayRun () {
	    hasReturns = false; // may or may not run
	    hasBreak.clear ();
	    hasThrow = false;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{hasReturns: " + hasReturns +
		", hasBreak: " + hasBreak + ", mayBreak: " + mayBreak +
		", hasThrow: " + hasThrow + ", mayThrow: " + mayThrow +
		", isInfiniteLoop: " + isInfiniteLoop + "}";
	}
    }

    private static class MethodData {
	private Set<String> activeLabels = new HashSet<> ();
	private Set<String> uninitializedLocals = new HashSet<> ();
    }
}
