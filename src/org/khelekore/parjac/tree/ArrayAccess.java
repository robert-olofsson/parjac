package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class ArrayAccess extends PositionNode {
    private TreeNode from;
    private final TreeNode exp;

    public ArrayAccess (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	from = parts.pop ();
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + from + "[" + exp + "]" + "}";
    }

    public TreeNode getFrom () {
	return from;
    }

    public void setFrom (TreeNode from) {
	this.from = from;
    }

    public TreeNode getArrayIndexExpression () {
	return exp;
    }

    @Override public void visit (TreeVisitor visitor) {
	from.visit (visitor);
	if (visitor.visit (this))
	    exp.visit (visitor);
    }

    @Override public ExpressionType getExpressionType () {
	ExpressionType et = from.getExpressionType ();
	return et.arrayAccess ();
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (exp);
    }
}
