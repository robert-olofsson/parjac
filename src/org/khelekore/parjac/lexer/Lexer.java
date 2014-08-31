package org.khelekore.parjac.lexer;

import java.nio.CharBuffer;

/** A somewhat simplified lexer for java, it skips the unicode escape handling. */
public class Lexer {
    // Typically the file name, used for error output
    private final String path;
    // We use the position for keeping track of where we are
    private final CharBuffer buf;
    private int tokenStartPosition = 0;
    private int currentLine = 0;
    private int currentColumn = 0;
    private boolean insideTypeContext = false;

    // Text set when we get an lexer ERROR
    private String errorText;
    private char currentCharValue;
    private String currentStringValue;

    public Lexer (String path, CharBuffer buf) {
	this.path = path;
	this.buf = buf.duplicate ();
    }

    public String getError () {
	return path + ":" + currentLine + ":" + currentColumn + ":" + errorText;
    }

    public char getCharValue () {
	return currentCharValue;
    }

    public String getCurrentStringValue () {
	return currentStringValue;
    }

    public char getCurrentCharValue () {
	return currentCharValue;
    }

    public void setInsideTypeContext (boolean insideTypeContext) {
	this.insideTypeContext = insideTypeContext;
    }

    public Token nextToken () {
	tokenStartPosition = buf.position ();
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
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
		return Token.SUB;

	    // separators
	    case '(':
		return Token.LEFT_PARANTHESIS;
	    case ')':
		return Token.RIGHT_PARANTHESIS;
	    case '{':
		return Token.LEFT_CURLY;
	    case '}':
		return Token.RIGHT_CURLY;
	    case '[':
		return Token.LEFT_BRACKET;
	    case ']':
		return Token.RIGHT_BRACKET;
	    case ';':
		return Token.SEMICOLON;
	    case ',':
		return Token.COMMA;
	    case '.':
		return handleDot ();  // not correct, may be start of floating point literal as well
	    case '@':
		return Token.AT;
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
		return handleExtraEqual (Token.NOT, Token.NOT_EQUAL);
	    case '~':
		return Token.TILDE;
	    case '?':
		return Token.QUESTIONMARK;
	    case '+':
		return handleDoubleOrEqual (c, Token.PLUS, Token.INCREMENT, Token.PLUS_EQUAL);
	    case '-':
		return handleMinus ();
	    case '*':
		return handleExtraEqual (Token.MULTIPLY, Token.MULTIPLY_EQUAL);
	    case '/':
		return handleSlash ();
	    case '%':
		return handleExtraEqual (Token.REMAINDER, Token.REMAINDER_EQUAL);
	    case '&':
		return handleDoubleOrEqual (c, Token.BIT_AND, Token.LOGICAL_AND, Token.BIT_AND_EQUAL);
	    case '|':
		return handleDoubleOrEqual (c, Token.BIT_OR, Token.LOGICAL_OR, Token.BIT_OR_EQUAL);
	    case '^':
		return handleExtraEqual (Token.BIT_XOR, Token.BIT_XOR_EQUAL);

	    case '\'':
		return readCharacterLiteral ();
	    case '"':
		return readStringLiteral ();
	    }
	}
	return Token.NULL;
    }

    public boolean hasMoreTokens () {
	return buf.position () != buf.limit ();
    }

    private Token readWhitespace () {
	char c = 0;
	while (buf.hasRemaining ()) {
	    c = nextChar ();
	    if (c != ' ' && c != '\t' && c != '\f') {
		pushBack ();
		break;
	    }
	}
	return Token.WHITESPACE;
    }

    private Token handleLF () { // easy case
	nextLine ();
	return Token.LF;
    }

    private Token handleCR () { // might be a CR or CRLF
	Token tt = handleOneExtra (Token.CR, '\n', Token.CRLF);
	nextLine ();
	return tt;
    }

    private Token handleDot () {
	Token tt = Token.DOT;
	if (buf.remaining () >= 2) {
	    buf.mark ();
	    char c2 = nextChar ();
	    if (c2 == '.') {
		char c3 = nextChar ();
		if (c3 == '.')
		    return Token.ELLIPSIS;
	    }
	    buf.reset ();
	}
	return tt;
    }

    private Token handleColon () {
	return handleOneExtra (Token.COLON, ':', Token.DOUBLE_COLON);
    }

    private Token handleEquals () {
	return handleOneExtra (Token.EQUAL, '=', Token.DOUBLE_EQUAL);
    }

    private Token handleExtraEqual (Token base, Token extra) {
	return handleOneExtra (base, '=', extra);
    }

    private Token handleDoubleOrEqual (char m, Token base, Token twice, Token baseEqual) {
	Token tt = base;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == m)
		tt = twice;
	    else if (c == '=')
		tt = baseEqual;
	    else
		pushBack ();
	}
	return tt;
    }

    private Token handleMinus () {
	// -, --, -=, ->
	Token tt = Token.MINUS;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '-')
		tt = Token.DECREMENT;
	    else if (c == '=')
		tt = Token.MINUS_EQUAL;
	    else if (c == '>')
		tt = Token.ARROW;
	    else
		pushBack ();
	}
	return tt;
    }

    private Token handleSlash () {
	// /, /=, //, /* ... */
	Token tt = Token.DIVIDE;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '=')
		tt = Token.DIVIDE_EQUAL;
	    else if (c == '/')
		tt = readOffOneLineComment ();
	    else if (c == '*')
		tt = readOffMultiLineComment ();
	    else
		pushBack ();
	}
	return tt;
    }

    private Token readOffOneLineComment () {
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '\n' || c == '\r') {
		pushBack ();
		break;
	    }
	}
	return Token.ONELINE_COMMENT;
    }

    private Token readOffMultiLineComment () {
	boolean previousWasStar = false;
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (previousWasStar && c == '/')
		return Token.MULTILINE_COMMENT;
	    previousWasStar = (c == '*');
	    if (c == '\n')
		handleLF ();
	    else if (c == '\r')
		handleCR ();
	}
	errorText = "Reached end of input while inside comment";
	return Token.ERROR;
    }

    private Token handleLT () {
	// <, <=, <<, <<=
	return handleLTGT ('<', Token.LT, Token.LE,
			   Token.LEFT_SHIFT, Token.LEFT_SHIFT_EQUAL);
    }

    private Token handleGT () {
	// >, >=, >>, >>=, >>>, >>>=
	Token tt = Token.GT;

	// generics
	if (insideTypeContext)
	    return tt;

	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '=') {
		tt = Token.GE;
	    } else if (c == '>') {
		tt = handleLTGT ('>', Token.RIGHT_SHIFT, Token.RIGHT_SHIFT_EQUAL,
				 Token.RIGHT_SHIFT_UNSIGNED, Token.RIGHT_SHIFT_UNSIGNED_EQUAL);
	    } else {
		pushBack ();
	    }
	}
	return tt;
    }

    private Token handleLTGT (char ltgt, Token base, Token baseEqual,
				  Token doubleBase, Token doubleBaseEqual) {
	Token tt = base;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '=')
		tt = baseEqual;
	    else if (c == ltgt)
		tt = handleOneExtra (doubleBase, '=', doubleBaseEqual);
	    else
		pushBack ();
	}
	return tt;
    }

    private Token handleOneExtra (Token base, char match, Token extended) {
	Token tt = base;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
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
	    handleString ('\'', Token.CHARACTER_LITERAL, "Character literal not closed");
	if (s == null)
	    return Token.ERROR;
	if (s.length () > 1) {
	    errorText = "Unclosed character literal: *" + s + "*";
	    return Token.ERROR;
	}
	currentCharValue = s.charAt (0);
	return Token.CHARACTER_LITERAL;
    }

    private Token readStringLiteral () {
	int pos = buf.position ();
	String s =
	    handleString ('"', Token.STRING_LITERAL, "String literal not closed");
	if (s == null)
	    return Token.ERROR;
	currentStringValue = s;
	return Token.STRING_LITERAL;
    }

    private String handleString (char end, Token base, String newlineError) {
	errorText = "End of input";

	Token tt = base;
	boolean previousWasBackslash = false;
	StringBuilder res = new StringBuilder ();

	while (buf.hasRemaining ()) {
	    char c = nextChar ();
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

    private char nextChar () {
	currentColumn++;
	return buf.get ();
    }

    private void pushBack () {
	currentColumn--;
	buf.position (buf.position () - 1);
    }

    private void nextLine () {
	currentLine++;
	currentColumn = 0;
    }
}