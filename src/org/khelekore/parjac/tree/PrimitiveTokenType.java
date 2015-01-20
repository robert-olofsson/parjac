package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.Token;

public class PrimitiveTokenType implements TreeNode {
    private final Token type;

    public PrimitiveTokenType (Token type) {
	this.type = type;
    }

    public Token get () {
	return type;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + "}";
    }
}