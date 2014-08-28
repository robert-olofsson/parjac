package org.khelekore.parjac.lexer;

public class Token {
    private final TokenType type;
    private final String value;

    public Token (TokenType type) {
	this.type = type;
	value = null;
    }

    public Token (TokenType type, String value) {
	this.type = type;
	this.value = value;
    }

    public TokenType getType () {
	return type;
    }
}
