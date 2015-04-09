package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class CharLiteral extends PositionNode {
    private final char value;

    public CharLiteral (char value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public char get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}