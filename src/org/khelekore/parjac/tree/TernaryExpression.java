package org.khelekore.parjac.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class TernaryExpression extends PositionNode {
    private final TreeNode test;
    private final TreeNode thenPart;
    private final TreeNode elsePart;
    private ExpressionType resultType;

    public TernaryExpression (TreeNode test, TreeNode thenPart, TreeNode elsePart, ParsePosition pos) {
	super (pos);
	this.test = test;
	this.thenPart = thenPart;
	this.elsePart = elsePart;
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 1)
	    return parts.pop ();
	TreeNode test = parts.pop ();
	parts.pop (); // '?'
	TreeNode thenPart = parts.pop ();
	parts.pop (); // ':'
	TreeNode elsePart = parts.pop ();
	return new TernaryExpression (test, thenPart, elsePart, pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + test + " ? " + thenPart + " : " + elsePart + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    test.visit (visitor);
	    thenPart.visit (visitor);
	    elsePart.visit (visitor);
	}
    }

    @Override public void simpleVisit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    public void setExpressionType (ExpressionType et) {
	resultType = et;
    }

    @Override public ExpressionType getExpressionType () {
	return resultType;
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	return Arrays.asList (test, thenPart, elsePart);
    }

    public TreeNode getExpression () {
	return test;
    }

    public TreeNode getThenPart () {
	return thenPart;
    }

    public TreeNode getElsePart () {
	return elsePart;
    }
}
