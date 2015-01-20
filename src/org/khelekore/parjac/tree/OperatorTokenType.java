package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.Token;

public class OperatorTokenType implements TreeNode {
    private final Token type;

    public OperatorTokenType (Token type) {
	this.type = type;
    }

    public Token get () {
	return type;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + "}";
    }
}