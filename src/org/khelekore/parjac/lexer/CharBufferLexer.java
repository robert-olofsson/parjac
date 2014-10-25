package org.khelekore.parjac.lexer;

import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.file.Path;

/** A lexer for the java language */
public class CharBufferLexer implements Lexer {
    // Typically the file name, used for error output
    private final Path path;
    // We use the position for keeping track of where we are
    private final CharBuffer buf;
    private long tokenStartPosition = 0;
    private long tokenStartColumn = 0;
    private long currentLine = 1;
    private long currentColumn = 0;
    private boolean insideTypeContext = false;

    // Text set when we get an lexer ERROR
    private String errorText;

    // The different values we can have
    private char currentCharValue;
    private String currentStringValue;
    private BigInteger currentIntValue; // for int and long
    private float currentFloatValue;
    private double currentDoubleValue;
    private String currentIdentifier;

    // Decimal values are always positive, hex, oct and binary may be negative
    private static final BigInteger MAX_INT_LITERAL = new BigInteger ("80000000", 16);
    private static final BigInteger MAX_LONG_LITERAL = new BigInteger ("8000000000000000", 16);
    private static final BigInteger MAX_UINT_LITERAL = new BigInteger  ("FFFFFFFF", 16);
    private static final BigInteger MAX_ULONG_LITERAL = new BigInteger ("FFFFFFFFFFFFFFFF", 16);

    public CharBufferLexer (Path path, CharBuffer buf) {
	this.path = path;
	this.buf = buf.duplicate ();
    }

    public String getError () {
	return errorText;
    }

    public char getCharValue () {
	return currentCharValue;
    }

    public String getStringValue () {
	return currentStringValue;
    }

    public int getIntValue () {
	// for 2^31 which is the max allowed int literal we get -2^31.
	// note however that (int)2^31 == (int)(-2^31)
	return currentIntValue.intValue ();
    }

    public long getLongValue () {
	// similar to int handling above
	return currentIntValue.longValue ();
    }

    public float getFloatValue () {
	return currentFloatValue;
    }

    public double getDoubleValue () {
	return currentDoubleValue;
    }

    public String getIdentifier () {
	return currentIdentifier;
    }

    public void setInsideTypeContext (boolean insideTypeContext) {
	this.insideTypeContext = insideTypeContext;
    }

    public long getLineNumber () {
	return currentLine;
    }

    public long getTokenStartPos () {
	return tokenStartPosition;
    }

    public long getTokenEndPos () {
	return buf.position ();
    }

    public long getTokenColumn () {
	return tokenStartColumn;
    }

    public Token nextToken () {
	tokenStartPosition = buf.position ();
	tokenStartColumn = currentColumn;
	if (buf.hasRemaining ()) {
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
		return Token.LEFT_PARENTHESIS;
	    case ')':
		return Token.RIGHT_PARENTHESIS;
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
		return handleDot ();
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
		return handleDoubleOrEqual (c, Token.AND, Token.LOGICAL_AND, Token.BIT_AND_EQUAL);
	    case '|':
		return handleDoubleOrEqual (c, Token.OR, Token.LOGICAL_OR, Token.BIT_OR_EQUAL);
	    case '^':
		return handleExtraEqual (Token.XOR, Token.BIT_XOR_EQUAL);

	    case '\'':
		return readCharacterLiteral ();
	    case '"':
		return readStringLiteral ();

	    case '0':
		return readZero ();
	    case '1':
	    case '2':
	    case '3':
	    case '4':
	    case '5':
	    case '6':
	    case '7':
	    case '8':
	    case '9':
		return readDecimalNumber (c);
	    default:
		if (Character.isJavaIdentifierStart (c))
		    return readIdentifier (c);

		errorText = "Illegal character: " + c + "(0x" + Integer.toHexString (c) + ")";
		return Token.ERROR;
	    }
	}
	return Token.END_OF_INPUT;
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
	if (buf.hasRemaining ()) {
	    buf.mark ();
	    char c2 = nextChar ();
	    if (c2 == '.') {
		if (buf.hasRemaining ()) {
		    char c3 = nextChar ();
		    if (c3 == '.')
			return Token.ELLIPSIS;
		}
	    } else if (c2 >= '0' && c2 <= '9') {
		StringBuilder value = new StringBuilder ();
		value.append ('.');
		value.append (c2);
		return readNumber (value, 10, true);
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
	String s =
	    handleString ('\'', Token.CHARACTER_LITERAL, "Character literal not closed");
	if (s == null)
	    return Token.ERROR;
	int len = s.length ();
	if (len == 0)
	    return Token.ERROR;
	if (len > 1) {
	    errorText = "Unclosed character literal: *" + s + "*";
	    return Token.ERROR;
	}
	currentCharValue = s.charAt (0);
	return Token.CHARACTER_LITERAL;
    }

    private Token readStringLiteral () {
	String s =
	    handleString ('"', Token.STRING_LITERAL, "String literal not closed");
	if (s == null)
	    return Token.ERROR;
	currentStringValue = s;
	return Token.STRING_LITERAL;
    }

    private String handleString (char end, Token base, String newlineError) {
	errorText = "End of input";

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

		// octal escape, Unicode \u0000 to \u00ff
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		    int i = getOctalEscape (c);
		    if (i >= 0 && i < 256)
			res.append ((char)i);
		    else
			errorText = "Invalid octal escape";
		    break;
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

    private int getOctalEscape (char start) {
	StringBuilder sb = new StringBuilder ();
	sb.append (start);
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c >= '0' && c <= '7') {
		sb.append (c);
	    } else {
		pushBack ();
		break;
	    }
	}
	try {
	    return Integer.parseInt (sb.toString (), 8);
	} catch (NumberFormatException n) {
	    return -1;
	}
    }

    private Token readZero () {
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == 'x') {
		return readNumber (new StringBuilder (), 16, false);
	    } else if (c == 'b') {
		return readNumber (new StringBuilder (), 2, false);
	    } else if (c == 'l' || c == 'L') {
		currentIntValue = BigInteger.ZERO;
		return Token.LONG_LITERAL;
	    } else if (c == '_' || (c >= '0' && c <= '7')) {
		StringBuilder value = new StringBuilder ();
		value.append (c);
		return readNumber (value, 8, false);
	    } else if (c == '.') {
		StringBuilder value = new StringBuilder ();
		value.append ('0');
		value.append (c);
		return readNumber (value, 10, true);
	    } else {
		currentIntValue = BigInteger.ZERO;
		pushBack ();
		return Token.INT_LITERAL;
	    }
	} else {
	    currentIntValue = BigInteger.ZERO;
	}
	return Token.INT_LITERAL;
    }

    private Token readDecimalNumber (char start) {
	StringBuilder res = new StringBuilder ();
	res.append (start);
	Token t = readNumber (res, 10, false);
	if (t == Token.ERROR)
	    return t;

	return t;
    }

    private Token readNumber (StringBuilder value, int radix, boolean hasSeenDot) {
	boolean lastWasUnderscore = false;
	boolean hasSeenExponent = false;
	Token type = Token.INT_LITERAL;
	char minChar = '0';
	char maxChar = (char)(minChar + Math.min (10, radix));
	while (buf.hasRemaining ()) {
	    lastWasUnderscore = false;
	    char c = nextChar ();
	    if (c >= minChar && c < maxChar) {
		value.append (c);
	    } else if (isAllowedHexDigit (radix, hasSeenExponent, c)) {
		value.append (c);
	    } else if (c == '_') { // skip it
		lastWasUnderscore = true;
	    } else if (c == 'd' || c == 'D') {
		type = Token.DOUBLE_LITERAL;
		break;
	    } else if (c == 'f' || c == 'F') {
		type = Token.FLOAT_LITERAL;
		break;
	    } else if (c == 'l' || c == 'L') {
		type = Token.LONG_LITERAL;
		break;
	    } else if (c == '.' && !hasSeenDot && (radix == 10 || radix == 16)) {
		hasSeenDot = true;
		value.append (c);
	    } else if (validExponent (radix, hasSeenExponent, c)) {
		hasSeenExponent = true;
		value.append (c);
		if (!readSignedInteger (value))
		    return Token.ERROR;
	    } else {
		pushBack ();
		break;
	    }
	}
	if (lastWasUnderscore) {
	    errorText = "Number may not end with underscore";
	    return Token.ERROR;
	}
	if (value.length () == 0) {
	    errorText = "Number may not be empty";
	    return Token.ERROR;
	}

	if ((hasSeenDot || hasSeenExponent) && type != Token.FLOAT_LITERAL)
	    type = Token.DOUBLE_LITERAL;
	if (type == Token.INT_LITERAL || type == Token.LONG_LITERAL)
	    return intValue (value.toString (), radix, type);
	return doubleValue (value.toString (), radix, type);
    }

    private boolean isAllowedHexDigit (int radix, boolean hasSeenExponent, char c) {
	if (radix != 16)
	    return false;
	// Exponents are decimal only
	if (hasSeenExponent)
	    return false;
	return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean validExponent (int radix, boolean hasSeenExponent, char c) {
	if (hasSeenExponent)
	    return false;
	if (radix == 10 && (c == 'e' || c == 'E'))
	    return true;
	if (radix == 16 && (c == 'p' || c == 'P'))
	    return true;
	return false;
    }

    private boolean readSignedInteger (StringBuilder value) {
	boolean lastWasUnderscore = false;
	boolean first = true;
	boolean foundDigits = false;
	while (buf.hasRemaining ()) {
	    lastWasUnderscore = false;
	    char c = nextChar ();
	    if (c >= '0' && c <= '9') {
		value.append (c);
		foundDigits = true;
	    } else if (c == '_') {
		lastWasUnderscore = true;
	    } else if (first && (c == '+' || c == '-')) {
		value.append (c);
	    } else {
		pushBack ();
		break;
	    }
	}
	if (lastWasUnderscore) {
	    errorText = "Number may not end with underscore";
	    return false;
	}
	if (!foundDigits) {
	    errorText = "Exponent not found";
	    return false;
	}
	return true;
    }

    private Token intValue (String text, int radix, Token type) {
	try {
	    currentIntValue = new BigInteger (text, radix);
	    BigInteger maxAllowed;
	    if (type == Token.INT_LITERAL)
		maxAllowed = radix == 10 ? MAX_INT_LITERAL : MAX_UINT_LITERAL;
	    else
		maxAllowed = radix == 10 ? MAX_LONG_LITERAL : MAX_ULONG_LITERAL;

	    if (currentIntValue.compareTo (maxAllowed) > 0) {
		errorText = "Integer literal too large";
		return Token.ERROR;
	    }
	    return type;
	} catch (NumberFormatException n) {
	    errorText = "Failed to parse int value: " + text;
	    return Token.ERROR;
	}
    }

    private Token doubleValue (String text, double radix, Token type) {
	if (radix == 16)
	    text = "0x" + text;
	try {
	    if (type == Token.DOUBLE_LITERAL)
		currentDoubleValue = Double.parseDouble (text);
	    else
		currentFloatValue = Float.parseFloat (text);
	    return type;
	} catch (NumberFormatException n) {
	    errorText = "Failed to parse floating point: " + text;
	    return Token.ERROR;
	}
    }

    private Token readIdentifier (char start) {
	StringBuilder res = new StringBuilder ();
	res.append (start);
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (Character.isJavaIdentifierPart (c)) {
		res.append (c);
	    } else {
		pushBack ();
		break;
	    }
	}
	String identifier = res.toString ();
	Token t = Token.getFromIdentifier (identifier);
	if (t != null)
	    return t;
	currentIdentifier = identifier;
	return Token.IDENTIFIER;
    }

    private char nextChar () {
	currentColumn++;
	char c = buf.get ();
	if (c == '\\') {
	    // check for unicode escapes
	    if (buf.remaining () > 4) {
		int p = buf.position ();
		if (buf.get (p++) == 'u') {
		    StringBuilder sb = new StringBuilder ();
		    for (int j = 0; j < 4; j++) {
			char h = buf.get (p++);
			if ((h >= '0' && h <= '9') || (h >= 'a' && h <= 'f') || (h >= 'A' && h <= 'F'))
			    sb.append (h);
			else
			    break;
		    }
		    if (sb.length () == 4) {
			int hv = Integer.parseInt (sb.toString (), 16);
			buf.position (p);
			c = (char)hv;
		    }
		}
	    }
	}
	return c;
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