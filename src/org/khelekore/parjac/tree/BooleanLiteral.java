package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class BooleanLiteral extends PositionNode {
    private final boolean value;

    public BooleanLiteral (boolean value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public boolean get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }
}