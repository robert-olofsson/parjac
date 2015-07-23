package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class StringLiteral extends PositionNode {
    private final String value;

    public StringLiteral (String value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public String get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }
}