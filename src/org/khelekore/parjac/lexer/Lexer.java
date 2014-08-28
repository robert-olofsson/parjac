package org.khelekore.parjac.lexer;

import java.nio.CharBuffer;

/** A somewhat simplified lexer for java, it skips the unicode escape handling. */
public class Lexer {
    // We use the position for keeping track of where we are
    private final CharBuffer buf;

    public Lexer (CharBuffer buf) {
	this.buf = buf.duplicate ();
    }

    public Token nextToken () {
	return new Token (TokenType.NULL);
    }
}