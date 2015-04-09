package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class FloatLiteral extends PositionNode {
    private final float value;

    public FloatLiteral (float value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public float get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}