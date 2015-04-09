package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class Identifier extends PositionNode {
    private final String value;

    public Identifier (String value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public String get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}