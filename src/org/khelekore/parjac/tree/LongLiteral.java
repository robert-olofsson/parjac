package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class LongLiteral extends PositionNode {
    private final long value;

    public LongLiteral (long value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public long get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}