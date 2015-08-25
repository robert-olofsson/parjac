package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class FloatLiteral extends PositionNode implements LiteralValue, NumericValue {
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

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public FloatLiteral getNegated () {
	return new FloatLiteral (-value, getParsePosition ());
    }

    @Override public String getExpressionType () {
	return "F";
    }
}