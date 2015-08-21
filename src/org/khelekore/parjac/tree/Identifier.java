package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class Identifier extends PositionNode {
    private final String value;
    private final String expressionType;

    public Identifier (String value, ParsePosition pos) {
	this (value, pos, null);
    }

    public Identifier (String value, ParsePosition pos, String type) {
	super (pos);
	this.value = value;
	this.expressionType = type;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + ", " + expressionType + "}";
    }

    public String get () {
	return value;
    }

    @Override public String getExpressionType () {
	return expressionType;
    }
}