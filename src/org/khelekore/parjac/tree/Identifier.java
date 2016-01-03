package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class Identifier extends PositionNode {
    private final String value;
    private final ExpressionType expressionType;

    public Identifier (String value, ParsePosition pos) {
	this (value, pos, null);
    }

    public Identifier (String value, ParsePosition pos, ExpressionType type) {
	super (pos);
	this.value = value;
	this.expressionType = type;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + ", expType: " + expressionType + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    public String get () {
	return value;
    }

    @Override public ExpressionType getExpressionType () {
	return expressionType;
    }
}