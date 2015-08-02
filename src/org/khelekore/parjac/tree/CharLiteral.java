package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class CharLiteral extends PositionNode implements LiteralValue, NumericValue {
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

    @Override public IntLiteral getNegated () {
	return new IntLiteral (-value, getParsePosition ());
    }

    @Override public Token getLiteralType () {
	return Token.CHAR;
    }
}