package org.khelekore.parjac.semantics;

import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.BasicForStatement;
import org.khelekore.parjac.tree.Block;
import org.khelekore.parjac.tree.BlockStatements;
import org.khelekore.parjac.tree.BooleanLiteral;
import org.khelekore.parjac.tree.BreakStatement;
import org.khelekore.parjac.tree.Catches;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.DoStatement;
import org.khelekore.parjac.tree.EnhancedForStatement;
import org.khelekore.parjac.tree.Finally;
import org.khelekore.parjac.tree.IfThenStatement;
import org.khelekore.parjac.tree.LiteralValue;
import org.khelekore.parjac.tree.MethodBody;
import org.khelekore.parjac.tree.MethodDeclaration;
import org.khelekore.parjac.tree.NullLiteral;
import org.khelekore.parjac.tree.PrimitiveTokenType;
import org.khelekore.parjac.tree.Result;
import org.khelekore.parjac.tree.ReturnStatement;
import org.khelekore.parjac.tree.SameTypeTreeVisitor;
import org.khelekore.parjac.tree.StringLiteral;
import org.khelekore.parjac.tree.SwitchBlock;
import org.khelekore.parjac.tree.SwitchBlockStatementGroup;
import org.khelekore.parjac.tree.SwitchLabel;
import org.khelekore.parjac.tree.SwitchStatement;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.ThrowStatement;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;
import org.khelekore.parjac.tree.TryStatement;
import org.khelekore.parjac.tree.UnannArrayType;
import org.khelekore.parjac.tree.WhileStatement;

public class ReturnChecker implements TreeVisitor {
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;

    public ReturnChecker (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void run () {
	tree.getCompilationUnit ().visit (this);
    }

    @Override public boolean visit (MethodDeclaration md) {
	MethodBody b = md.getBody ();
	if (!b.isEmpty ()) {
	    if (!checkBlock (b.getBlock (), md.getResult ()))
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     b.getParsePosition (),
							     "Method missing return"));
	}
	return true;
    }

    private boolean checkBlock (Block b, Result res) {
	return checkStatements (b.getStatements (), res);
    }

    private boolean checkStatements (List<TreeNode> statements, Result res) {
	boolean ends = false;
	if (statements != null) {
	    StatementChecker sc = new StatementChecker (res);
	    for (int i = 0, s = statements.size (); i < s; i++) {
		sc.clear ();
		TreeNode tn = statements.get (i);
		tn.visit (sc);
		ends = sc.ends;
		if (ends && i < s -1) {
		    TreeNode next = statements.get (i + 1);
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
								 next.getParsePosition (),
								 "Unreachable code"));
		}
	    }
	}
	return ends || res instanceof Result.VoidResult;
    }

    private class StatementChecker extends SameTypeTreeVisitor {
	private final Result res;
	private boolean ends;

	public StatementChecker (Result res) {
	    this.res = res;
	}

	public void clear () {
	    ends = false;
	}

	@Override public boolean visit (ReturnStatement r) {
	    ends = true;
	    checkType (r, res);
	    return false;
	}

	@Override public void visit (ThrowStatement t) {
	    ends = true;
	}

	@Override public void visit (BreakStatement b) {
	    ends = true;
	}

	@Override public boolean visit (IfThenStatement i) {
	    boolean statementEnds = checkStatement (i.getIfStatement ());
	    boolean elseEnds = false;
	    TreeNode elseStatement = i.getElseStatement ();
	    if (elseStatement != null)
		elseEnds = checkStatement (elseStatement);
	    ends = statementEnds && elseEnds;
	    return false;
	}

	@Override public boolean visit (WhileStatement w) {
	    checkExpressionStatement (w.getExpression (), w.getStatement ());
	    return false;
	}

	@Override public boolean visit (DoStatement d) {
	    checkExpressionStatement (d.getExpression (), d.getStatement ());
	    return false;
	}

	@Override public boolean visit (BasicForStatement f) {
	    checkExpressionStatement (f.getExpression (), f.getStatement ());
	    return false;
	}

	@Override public boolean visit (EnhancedForStatement f) {
	    checkStatement (f.getStatement ());
	    return false;
	}

	private void checkExpressionStatement (TreeNode exp, TreeNode statement) {
	    checkStatement (statement);
	    if (isTrue (exp))
		ends = true;   // infinite loop!
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
	    boolean lastEnds = true;
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
	    if (lastEnds && hasDefault)
		ends = true;
	    return false;
	}

	private boolean checkStatement (TreeNode t) {
	    if (t instanceof ReturnStatement) {
		visit ((ReturnStatement)t);
		return true;
	    }
	    if (t instanceof Block)
		return checkBlock ((Block)t, res);
	    if (t instanceof ThrowStatement)
		return true;
	    if (t instanceof BlockStatements)
		return checkStatements (((BlockStatements)t).get (), res);
	    return false;
	}

	private boolean hasDefault (List<SwitchLabel> labels) {
	    return labels.stream ().anyMatch (l -> l instanceof SwitchLabel.DefaultLabel);
	}

	@Override public boolean visit (TryStatement t) {
	    Block block = t.getBlock ();
	    boolean blockEnds = checkBlock (block, res);
	    boolean finallyEnds = false;
	    Catches c = t.getCatches ();
	    if (c != null)
		c.get ().forEach (cc -> checkBlock (cc.getBlock (), res));
	    Finally f = t.getFinallyBlock ();
	    if (f != null)
		finallyEnds = checkBlock (f.getBlock (), res);
	    if (finallyEnds) {
		diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (),
							       f.getParsePosition (),
							       "Return in finally"));
		ends = true;
	    } else if (blockEnds) {
		ends = true;
	    }
	    return false;
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
							     "Wrong return type"));
	    }
	}
    }

    private boolean match (TreeNode result, TreeNode exp) {
	if (exp instanceof LiteralValue) {
	    LiteralValue lv = (LiteralValue)exp;
	    Token expToken = lv.getLiteralType ();

	    if (result instanceof PrimitiveTokenType) {
		PrimitiveTokenType ptt = (PrimitiveTokenType)result;
		Token resultToken = ptt.get ();
		if (resultToken == expToken)
		    return true;
	    } else {
		return false;
	    }
	}

	// TODO: check result type better, but need types set on statements first
	if (result instanceof PrimitiveTokenType) {
	    // TODO: check exp type
	} else {
	    if (exp instanceof NullLiteral)
		return true;
	    if (result instanceof ClassType) {
		ClassType ct = (ClassType)result;
		if (ct.getFullName ().equals ("java.lang.String") &&
		    exp instanceof StringLiteral)
		    return true;
		// TODO: check other class match
	    } else if (result instanceof UnannArrayType) {
		// TODO: check this
	    }
	}
	return false;
    }
}
