package org.khelekore.parjac.lexer;

import java.nio.CharBuffer;
import org.testng.annotations.Test;

public class TestLexer {
    @Test
    public void testNullLiteral () {
	CharBuffer cb = CharBuffer.wrap ("null".toCharArray ());
	Lexer l = new Lexer (cb);
	Token t = l.nextToken ();
	assert t != null : "Returned token may not be null";
	TokenType tt = t.getType ();
	assert tt != null : "TokenType may not be null";
	assert tt == TokenType.NULL;
    }
}