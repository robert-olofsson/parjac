package org.khelekore.parjac.tree;

import java.util.Collection;

import org.khelekore.parjac.lexer.ParsePosition;

public class Identifier extends PositionNode {
    private final String value;
    private final ExpressionType expressionType;
    private TreeNode actual;

    public Identifier (String value, ParsePosition pos) {
	this (value, pos, null);
    }

    public Identifier (String value, ParsePosition pos, ExpressionType type) {
	super (pos);
	this.value = value;
	this.expressionType = type;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + ", expType: " + expressionType +
	    (actual != null ? ", actual: " + actual : "") + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (actual != null)
	    actual.visit (visitor);
	else
	    visitor.visit (this);
    }

    public String get () {
	return value;
    }

    @Override public ExpressionType getExpressionType () {
	if (actual != null)
	    return actual.getExpressionType ();
	return expressionType;
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	if (actual == null)
	    return super.getChildNodes ();
	return actual.getChildNodes ();
    }

    public void setActual (TreeNode actual) {
	this.actual = actual;
    }
}