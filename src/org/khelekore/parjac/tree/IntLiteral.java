package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class IntLiteral extends PositionNode implements LiteralValue, NumericValue {
    private final int value;

    public IntLiteral (int value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public int get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public IntLiteral getNegated () {
	return new IntLiteral (-value, getParsePosition ());
    }

    @Override public Token getLiteralType () {
	return Token.INT;
    }
}