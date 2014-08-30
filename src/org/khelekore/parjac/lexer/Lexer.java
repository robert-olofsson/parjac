package org.khelekore.parjac.lexer;

import java.nio.CharBuffer;

/** A somewhat simplified lexer for java, it skips the unicode escape handling. */
public class Lexer {
    // We use the position for keeping track of where we are
    private final CharBuffer buf;
    private int tokenStartPosition = 0;
    private int currentLine = 0;
    private int currentColumn = 0;

    public Lexer (CharBuffer buf) {
	this.buf = buf.duplicate ();
    }

    public Token nextToken () {
	tokenStartPosition = buf.position ();
	while (buf.hasRemaining ()) {
	    char c = buf.get ();
	    switch (c) {

	    case ' ':
	    case '\t':
	    case '\f':
		return readWhitespace ();

	    case '\n':
		return handleLF ();
	    case '\r':
		return handleCR ();

	    case '\u001a':
		return getToken (TokenType.SUB);

	    case '(':
		return getToken (TokenType.LEFT_PARANTHESIS);
	    case ')':
		return getToken (TokenType.RIGHT_PARANTHESIS);
	    case '{':
		return getToken (TokenType.LEFT_CURLY);
	    case '}':
		return getToken (TokenType.RIGHT_CURLY);
	    case '[':
		return getToken (TokenType.LEFT_BRACKET);
	    case ']':
		return getToken (TokenType.RIGHT_BRACKET);
	    case ';':
		return getToken (TokenType.SEMICOLON);
	    case ',':
		return getToken (TokenType.COMMA);
	    case '.':
		return handleDot ();
	    case '@':
		return getToken (TokenType.AT);
	    case ':':
		return handleColon ();

	    }
	}
	return new Token (TokenType.NULL);
    }

    public boolean hasMoreTokens () {
	return buf.position () != buf.limit ();
    }

    private Token readWhitespace () {
	char c = 0;
	while (buf.hasRemaining ()) {
	    c = buf.get ();
	    if (c != ' ' && c != '\t' && c != '\f') {
		buf.position (buf.position () - 1);
		break;
	    }
	}
	return getToken (TokenType.WHITESPACE);
    }

    private Token handleLF () { // easy case
	currentLine++;
	currentColumn = 0;
	return getToken (TokenType.LF);
    }

    private Token handleCR () { // might be a CR or CRLF
	TokenType tt = handleOneExtra (TokenType.CR, '\n', TokenType.CRLF);
	currentLine++;
	currentColumn = 0;
	return getToken (tt);
    }

    private Token handleDot () {
	TokenType tt = TokenType.DOT;
	if (buf.remaining () >= 2) {
	    buf.mark ();
	    char c2 = buf.get ();
	    if (c2 == '.') {
		char c3 = buf.get ();
		if (c3 == '.')
		    return getToken (TokenType.ELLIPSIS);
	    }
	    buf.reset ();
	}
	return getToken (tt);
    }

    private Token handleColon () {
	return getToken (handleOneExtra (TokenType.COLON, ':', TokenType.DOUBLE_COLON));
    }

    private TokenType handleOneExtra (TokenType base, char match, TokenType extended) {
	TokenType tt = base;
	if (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (c == match)
		tt = extended;
	    else // push back what we read
		buf.position (buf.position () - 1);
	}
	return tt;
    }

    private Token getToken (TokenType tt) {
	return new Token (tt); // fill in value?
    }
}