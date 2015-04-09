package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public abstract class TokenTypeBase extends PositionNode {
    private final Token type;

    public TokenTypeBase (Token type, ParsePosition pos) {
	super (pos);
	this.type = type;
    }

    public Token get () {
	return type;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + "}";
    }
}