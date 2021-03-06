package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class DoubleLiteral extends PositionNode implements LiteralValue, NumericValue {
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

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public DoubleLiteral getNegated () {
	return new DoubleLiteral (-value, getParsePosition ());
    }

    @Override public ExpressionType getExpressionType () {
	return ExpressionType.DOUBLE;
    }
}