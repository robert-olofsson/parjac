package org.khelekore.parjac.lexer;

import java.nio.CharBuffer;

/** A somewhat simplified lexer for java, it skips the unicode escape handling. */
public class Lexer {
    // We use the position for keeping track of where we are
    private final CharBuffer buf;
    private int tokenStartPosition = 0;
    private int currentLine = 0;
    private int currentColumn = 0;
    private boolean insideTypeContext = false;

    // Text set when we get an lexer ERROR
    private String errorText;

    public Lexer (CharBuffer buf) {
	this.buf = buf.duplicate ();
    }

    public String getError () {
	return errorText;
    }

    public void setInsideTypeContext (boolean insideTypeContext) {
	this.insideTypeContext = insideTypeContext;
    }

    public Token nextToken () {
	tokenStartPosition = buf.position ();
	while (buf.hasRemaining ()) {
	    char c = buf.get ();
	    switch (c) {

	    // whitespace
	    case ' ':
	    case '\t':
	    case '\f':
		return readWhitespace ();

	    // newlines
	    case '\n':
		return handleLF ();
	    case '\r':
		return handleCR ();

	     // sub
	    case '\u001a':
		return getToken (TokenType.SUB);

	    // separators
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
	    case ':':  // : is an operator, :: is a separator
		return handleColon ();

	    // operators (and comments)
	    case '=':
		return handleEquals ();
	    case '>':
		return handleGT ();
	    case '<':
		return handleLT ();
	    case '!':
		return handleExtraEqual (TokenType.NOT, TokenType.NOT_EQUAL);
	    case '~':
		return getToken (TokenType.TILDE);
	    case '?':
		return getToken (TokenType.QUESTIONMARK);
	    case '+':
		return handleDoubleOrEqual (c, TokenType.PLUS, TokenType.INCREMENT, TokenType.PLUS_EQUAL);
	    case '-':
		return handleMinus ();
	    case '*':
		return handleExtraEqual (TokenType.MULTIPLY, TokenType.MULTIPLY_EQUAL);
	    case '/':
		return handleSlash ();
	    case '%':
		return handleExtraEqual (TokenType.REMAINDER, TokenType.REMAINDER_EQUAL);
	    case '&':
		return handleDoubleOrEqual (c, TokenType.BIT_AND, TokenType.LOGICAL_AND, TokenType.BIT_AND_EQUAL);
	    case '|':
		return handleDoubleOrEqual (c, TokenType.BIT_OR, TokenType.LOGICAL_OR, TokenType.BIT_OR_EQUAL);
	    case '^':
		return handleExtraEqual (TokenType.BIT_XOR, TokenType.BIT_XOR_EQUAL);

	    case '\'':
		return readCharacterLiteral ();
	    case '"':
		return readStringLiteral ();
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
		pushBack ();
		break;
	    }
	}
	return getToken (TokenType.WHITESPACE);
    }

    private Token handleLF () { // easy case
	nextLine ();
	return getToken (TokenType.LF);
    }

    private Token handleCR () { // might be a CR or CRLF
	TokenType tt = handleOneExtra (TokenType.CR, '\n', TokenType.CRLF);
	nextLine ();
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

    private Token handleEquals () {
	return getToken (handleOneExtra (TokenType.EQUAL, '=', TokenType.DOUBLE_EQUAL));
    }

    private Token handleExtraEqual (TokenType base, TokenType extra) {
	return getToken (handleOneExtra (base, '=', extra));
    }

    private Token handleDoubleOrEqual (char m, TokenType base, TokenType twice, TokenType baseEqual) {
	TokenType tt = base;
	if (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (c == m)
		tt = twice;
	    else if (c == '=')
		tt = baseEqual;
	    else
		pushBack ();
	}
	return getToken (tt);
    }

    private Token handleMinus () {
	// -, --, -=, ->
	TokenType tt = TokenType.MINUS;
	if (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (c == '-')
		tt = TokenType.DECREMENT;
	    else if (c == '=')
		tt = TokenType.MINUS_EQUAL;
	    else if (c == '>')
		tt = TokenType.ARROW;
	    else
		pushBack ();
	}
	return getToken (tt);
    }

    private Token handleSlash () {
	// /, /=, //, /* ... */
	TokenType tt = TokenType.DIVIDE;
	if (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (c == '=')
		tt = TokenType.DIVIDE_EQUAL;
	    else if (c == '/')
		tt = readOffOneLineComment ();
	    else if (c == '*')
		tt = readOffMultiLineComment ();
	    else
		pushBack ();
	}
	return getToken (tt);
    }

    private TokenType readOffOneLineComment () {
	while (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (c == '\n' || c == '\r') {
		pushBack ();
		break;
	    }
	}
	return TokenType.ONELINE_COMMENT;
    }

    private TokenType readOffMultiLineComment () {
	boolean previousWasStar = false;
	while (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (previousWasStar && c == '/')
		return TokenType.MULTILINE_COMMENT;
	    previousWasStar = (c == '*');
	    if (c == '\n')
		handleLF ();
	    else if (c == '\r')
		handleCR ();
	}
	errorText = "Reached end of input while inside comment";
	return TokenType.ERROR;
    }

    private Token handleLT () {
	// <, <=, <<, <<=
	TokenType tt = handleLTGT ('<', TokenType.LT, TokenType.LE,
				   TokenType.LEFT_SHIFT, TokenType.LEFT_SHIFT_EQUAL);
	return getToken (tt);
    }

    private Token handleGT () {
	// >, >=, >>, >>=, >>>, >>>=
	TokenType tt = TokenType.GT;

	// generics
	if (insideTypeContext)
	    return getToken (tt);

	if (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (c == '=') {
		tt = TokenType.GE;
	    } else if (c == '>') {
		tt = handleLTGT ('>', TokenType.RIGHT_SHIFT, TokenType.RIGHT_SHIFT_EQUAL,
				 TokenType.RIGHT_SHIFT_UNSIGNED, TokenType.RIGHT_SHIFT_UNSIGNED_EQUAL);
	    } else {
		pushBack ();
	    }
	}
	return getToken (tt);
    }

    private TokenType handleLTGT (char ltgt, TokenType base, TokenType baseEqual,
				  TokenType doubleBase, TokenType doubleBaseEqual) {
	TokenType tt = base;
	if (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (c == '=')
		tt = baseEqual;
	    else if (c == ltgt)
		tt = handleOneExtra (doubleBase, '=', doubleBaseEqual);
	    else
		pushBack ();
	}
	return tt;
    }

    private TokenType handleOneExtra (TokenType base, char match, TokenType extended) {
	TokenType tt = base;
	if (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (c == match)
		tt = extended;
	    else // push back what we read
		pushBack ();
	}
	return tt;
    }

    private Token readCharacterLiteral () {
	int pos = buf.position ();
	String s =
	    handleString ('\'', TokenType.CHARACTER_LITERAL, "Character literal not closed");
	if (s == null)
	    return getToken (TokenType.ERROR);
	if (s.length () > 1) {
	    errorText = "Unclosed character literal: *" + s + "*";
	    return getToken (TokenType.ERROR);
	}
	return getToken (TokenType.CHARACTER_LITERAL);
    }

    private Token readStringLiteral () {
	int pos = buf.position ();
	String s =
	    handleString ('"', TokenType.STRING_LITERAL, "String literal not closed");
	if (s == null)
	    return getToken (TokenType.ERROR);
	return getToken (TokenType.STRING_LITERAL);
    }

    private String handleString (char end, TokenType base, String newlineError) {
	errorText = "End of input";

	TokenType tt = base;
	boolean previousWasBackslash = false;
	StringBuilder res = new StringBuilder ();

	while (buf.hasRemaining ()) {
	    char c = buf.get ();
	    if (previousWasBackslash) {
		switch (c) {
		case 'b': res.append ('\b'); break;
		case 't': res.append ('\t'); break;
		case 'n': res.append ('\n'); break;
		case 'f': res.append ('\f'); break;
		case 'r': res.append ('\r'); break;
		case '"':  // fall through
		case '\'': // fall through
		case '\\': res.append (c); break;
		default:
		    res.append ('\\');
		    res.append (c);
		    errorText = "Illegal escape sequence";
		}
		previousWasBackslash = false;
	    } else if (c == '\n' || c == '\r') {
		errorText = newlineError;
		pushBack ();
	    } else if (c == end) {
		errorText = null;
		break;
	    } else if (c == '\\') {
		previousWasBackslash = true;
	    } else {
		res.append (c);
	    }
	}
	return errorText == null ? res.toString () : null;
    }

    private void pushBack () {
	buf.position (buf.position () - 1);
    }

    private void nextLine () {
	currentLine++;
	currentColumn = 0;
    }

    private Token getToken (TokenType tt) {
	return new Token (tt); // fill in value?
    }
}