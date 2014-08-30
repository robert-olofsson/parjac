package org.khelekore.parjac.lexer;

import java.nio.CharBuffer;
import java.util.Arrays;
import org.testng.annotations.Test;

public class TestLexer {
    @Test
    public void testLF () {
	testInput ("\n", TokenType.LF);
    }

    @Test
    public void testCR () {
	testInput ("\r", TokenType.CR);
	testInput ("\r \n", TokenType.CR, TokenType.WHITESPACE, TokenType.LF);
    }

    @Test
    public void testCRLF () {
	testInput ("\r\n", TokenType.CRLF);
    }

    @Test
    public void testLFCR () {
	CharBuffer cb = CharBuffer.wrap ("\n\r".toCharArray ());
	Lexer l = new Lexer (cb);
	assert l.nextToken ().getType () == TokenType.LF;
	assert l.nextToken ().getType () == TokenType.CR;
    }

    @Test
    public void testColon () {
	testInput (":", TokenType.COLON);
	testInput (": :", TokenType.COLON, TokenType.WHITESPACE, TokenType.COLON);
    }

    @Test
    public void testDoubleColon () {
	testInput ("::", TokenType.DOUBLE_COLON);
	testInput (":::", TokenType.DOUBLE_COLON, TokenType.COLON);
	testInput ("::::", TokenType.DOUBLE_COLON, TokenType.DOUBLE_COLON);
	testInput (":: ::", TokenType.DOUBLE_COLON, TokenType.WHITESPACE, TokenType.DOUBLE_COLON);
    }

    @Test
    public void testSpaceWhitespace () {
	testInput (" ", TokenType.WHITESPACE);
	testInput ("    ", TokenType.WHITESPACE);
    }

    @Test
    public void testTabSpaceWhitespace () {
	testInput ("\t", TokenType.WHITESPACE);
    }

    @Test
    public void testFormFeedSpaceWhitespace () {
	testInput ("\f", TokenType.WHITESPACE);
    }

    @Test
    public void testMixedSpaceWhitespace () {
	testInput ("\t\t\t    ", TokenType.WHITESPACE);
    }

    @Test
    public void testSub () {
	testInput ("\u001a", TokenType.SUB);
    }

    /* Not working yet
    @Test
    public void testNullLiteral () {
	testInput ("null", TokenType.NULL);
    }

    @Test
    public void testNonNullLiteral () {
	testInput ("null_a", TokenType.IDENTIFIER);
    }

    @Test
    public void testTRUE () {
	testInput ("true", TokenType.TRUE);
    }

    @Test
    public void testFALSE () {
	testInput ("false", TokenType.FALSE);
    }

    @Test
    public void testIntLiterals () {
	String text = "0 2 0372 0xDada_Cafe 1996 0x00_FF__00_FF";
	CharBuffer cb = CharBuffer.wrap (text.toCharArray ());
	Lexer l = new Lexer (cb);
	while (l.hasMoreTokens ()) {
	    Token t = l.nextToken ();
	    TokenType tt = t.getType ();
	    assert (tt == TokenType.INTEGER_LITERAL || tt == TokenType.WHITESPACE)
		: "Wrong TokenType: " + tt;
	}
    }
    */

    private void testInput (String text, TokenType... expected) {
	CharBuffer cb = CharBuffer.wrap (text.toCharArray ());
	Lexer l = new Lexer (cb);
	for (int i = 0; i < expected.length; i++) {
	    assert l.hasMoreTokens () : "Too few tokens in text: " + text +
		", expected: " + Arrays.toString (expected);
	    Token t = l.nextToken ();
	    assert t != null : "Returned token may not be null";
	    TokenType tt = t.getType ();
	    assert tt != null : "TokenType may not be null";
	    assert tt == expected[i] : "Wrong TokenType: expected: " + expected[i] + ", got: " + tt;
	}
    }
}