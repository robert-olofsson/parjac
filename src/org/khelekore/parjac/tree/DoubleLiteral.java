package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class DoubleLiteral extends PositionNode {
    private final double value;

    public DoubleLiteral (double value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public double get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}