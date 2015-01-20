package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.Token;

public abstract class TokenTypeBase implements TreeNode {
    private final Token type;

    public TokenTypeBase (Token type) {
	this.type = type;
    }

    public Token get () {
	return type;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + "}";
    }
}