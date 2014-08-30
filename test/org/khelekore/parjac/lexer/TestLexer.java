package org.khelekore.parjac.lexer;

import java.nio.CharBuffer;
import java.util.Arrays;
import org.testng.annotations.Test;

public class TestLexer {

    @Test
    public void testSpaceWhitespace () {
	testInput (" ", TokenType.WHITESPACE);
	testInput ("    ", TokenType.WHITESPACE);
	testInput ("\t", TokenType.WHITESPACE);
	testInput ("\f", TokenType.WHITESPACE);
	testInput ("\t\t\t    ", TokenType.WHITESPACE);
    }

    @Test
    public void testNewlines () {
	testInput ("\n", TokenType.LF);
	testInput ("\r", TokenType.CR);
	testInput ("\r \n", TokenType.CR, TokenType.WHITESPACE, TokenType.LF);
	testInput ("\r\n", TokenType.CRLF);
	testInput ("\n\r", TokenType.LF, TokenType.CR);
    }

    @Test
    public void testSub () {
	testInput ("\u001a", TokenType.SUB);
    }

    @Test
    public void testColon () {
	testInput (":", TokenType.COLON);
	testInput (": :", TokenType.COLON, TokenType.WHITESPACE, TokenType.COLON);
	testInput ("::", TokenType.DOUBLE_COLON);
	testInput (":::", TokenType.DOUBLE_COLON, TokenType.COLON);
	testInput ("::::", TokenType.DOUBLE_COLON, TokenType.DOUBLE_COLON);
	testInput (":: ::", TokenType.DOUBLE_COLON, TokenType.WHITESPACE, TokenType.DOUBLE_COLON);
    }

    @Test
    public void testDot () {
	testInput (".", TokenType.DOT);
	testInput ("..", TokenType.DOT, TokenType.DOT);
	testInput ("...", TokenType.ELLIPSIS);
    }

    @Test
    public void testSeparators () {
	testInput ("(", TokenType.LEFT_PARANTHESIS);
	testInput (")", TokenType.RIGHT_PARANTHESIS);
	testInput ("{", TokenType.LEFT_CURLY);
	testInput ("}", TokenType.RIGHT_CURLY);
	testInput ("[", TokenType.LEFT_BRACKET);
	testInput ("]", TokenType.RIGHT_BRACKET);
	testInput (";", TokenType.SEMICOLON);
	testInput (",", TokenType.COMMA);
	testInput ("@", TokenType.AT);
    }

    @Test
    public void testOperators () {
	testInput ("=", TokenType.EQUAL);
	testInput ("= =", TokenType.EQUAL, TokenType.WHITESPACE, TokenType.EQUAL);
	testInput (">", TokenType.GT);
	testInput ("<", TokenType.LT);
	testInput ("!", TokenType.NOT);
	testInput ("~", TokenType.TILDE);
	testInput ("?", TokenType.QUESTIONMARK);
	testInput (":", TokenType.COLON);
	testInput ("->", TokenType.ARROW);
	testInput ("==", TokenType.DOUBLE_EQUAL);
	testInput (">=", TokenType.GE);
	testInput ("<=", TokenType.LE);
	testInput ("!=", TokenType.NOT_EQUAL);
	testInput ("&&", TokenType.LOGICAL_AND);
	testInput ("||", TokenType.LOGICAL_OR);
	testInput ("++", TokenType.INCREMENT);
	testInput ("--", TokenType.DECREMENT);
	testInput ("+", TokenType.PLUS);
	testInput ("-", TokenType.MINUS);
	testInput ("*", TokenType.MULTIPLY);
	testInput ("/", TokenType.DIVIDE);
	testInput ("&", TokenType.BIT_AND);
	testInput ("|", TokenType.BIT_OR);
	testInput ("^", TokenType.BIT_XOR);
	testInput ("%", TokenType.REMAINDER);
	testInput ("<<", TokenType.LEFT_SHIFT);
	testInput (">>", TokenType.RIGHT_SHIFT);
	testInput (">>>", TokenType.RIGHT_SHIFT_UNSIGNED);
	testInput ("+=", TokenType.PLUS_EQUAL);
	testInput ("-=", TokenType.MINUS_EQUAL);
	testInput ("*=", TokenType.MULTIPLY_EQUAL);
	testInput ("/=", TokenType.DIVIDE_EQUAL);
	testInput ("&=", TokenType.BIT_AND_EQUAL);
	testInput ("|=", TokenType.BIT_OR_EQUAL);
	testInput ("^=", TokenType.BIT_XOR_EQUAL);
	testInput ("%=", TokenType.REMAINDER_EQUAL);
	testInput ("<<=", TokenType.LEFT_SHIFT_EQUAL);
	testInput (">>=", TokenType.RIGHT_SHIFT_EQUAL);
	testInput (">>>=", TokenType.RIGHT_SHIFT_UNSIGNED_EQUAL);
    }

    @Test
    public void testGenerics () {
	CharBuffer cb = CharBuffer.wrap (">>".toCharArray ());
	Lexer l = new Lexer ("TestLexer", cb);
	l.setInsideTypeContext (true);
	testLexing (l, TokenType.GT, TokenType.GT);
    }

    @Test
    public void testComment () {
	testInput ("// whatever", TokenType.ONELINE_COMMENT);
	testInput ("/* whatever */", TokenType.MULTILINE_COMMENT);
	testInput ("/* this comment /* // /** ends here: */", TokenType.MULTILINE_COMMENT);
	testInput ("/* whatever", TokenType.ERROR);
	testInput ("/* whatever \n whatever */", TokenType.MULTILINE_COMMENT);
	testInput ("/* whatever \n * whatever \n *\n/*/", TokenType.MULTILINE_COMMENT);
    }

    @Test
    public void testCharLiteral () {
	testChar ("'a'", 'a');
	testChar ("'\\\\'", '\\');
	testChar ("'\\t'", '\t');
	// Does not handle octal escapes yet, silly thing
	// testInput ("'\\12'", TokenType.CHARACTER_LITERAL); // octal escape
	testInput ("'\\a'", TokenType.ERROR);
	testInput ("'ab'", TokenType.ERROR);
	testInput ("'a", TokenType.ERROR);
    }

    private void testChar (String toLex, char value) {
	CharBuffer cb = CharBuffer.wrap (toLex.toCharArray ());
	Lexer l = new Lexer ("TestLexer", cb);
	testLexing (l, TokenType.CHARACTER_LITERAL);
	char res = l.getCurrentCharValue ();
	assert value == res : "Wrong string value: " + res;
    }

    @Test
    public void testStringLiteral () {
	testString ("\"\"", "");
	testString ("\"abc123\"", "abc123");
    }

    private void testString (String toLex, String value) {
	CharBuffer cb = CharBuffer.wrap (toLex.toCharArray ());
	Lexer l = new Lexer ("TestLexer", cb);
	testLexing (l, TokenType.STRING_LITERAL);
	String res = l.getCurrentStringValue ();
	assert value.equals (res) : "Wrong string value: " + res;
    }

    @Test
    public void testComplex () {
	// nonsense, but valid for tokenisation
	testInput ("{    \n    (/*whatever*/)=[]}",
		   TokenType.LEFT_CURLY, TokenType.WHITESPACE,
		   TokenType.LF, TokenType.WHITESPACE,
		   TokenType.LEFT_PARANTHESIS, TokenType.MULTILINE_COMMENT,
		   TokenType.RIGHT_PARANTHESIS, TokenType.EQUAL,
		   TokenType.LEFT_BRACKET, TokenType.RIGHT_BRACKET,
		   TokenType.RIGHT_CURLY);
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
	Lexer l = new Lexer ("TestLexer", cb);
	testLexing (l, expected);
    }

    private void testLexing (Lexer l, TokenType... expected) {
	for (int i = 0; i < expected.length; i++) {
	    assert l.hasMoreTokens () : "Too few tokens, expected: " + Arrays.toString (expected);
	    Token t = l.nextToken ();
	    assert t != null : "Returned token may not be null";
	    TokenType tt = t.getType ();
	    assert tt != null : "TokenType may not be null";
	    assert tt == expected[i] : "Wrong TokenType: expected: " + expected[i] + ", got: " + tt
		+ (tt == TokenType.ERROR ? ", error code: " + l.getError () : "");
	}
	assert !l.hasMoreTokens () : "Lexer has more available tokens than expected";
    }
}