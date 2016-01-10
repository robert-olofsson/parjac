package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class LongLiteral extends PositionNode implements LiteralValue, NumericValue {
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

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public LongLiteral getNegated () {
	return new LongLiteral (-value, getParsePosition ());
    }

    @Override public ExpressionType getExpressionType () {
	return ExpressionType.LONG;
    }
}