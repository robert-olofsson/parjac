package org.khelekore.parjac.lexer;

public interface Lexer {

    /** Get the current error */
    String getError ();

    /** Get the value of the current character token */
    char getCharValue ();

    /** Get the value of the current String token */
    String getStringValue ();

    /** Get the value of the current int token */
    int getIntValue ();

    /** Get the value of the current long token */
    long getLongValue ();

    /** Get the value of the current float token */
    float getFloatValue ();

    /** Get the value of the current double token */
    double getDoubleValue ();

    /** Get the start position of the current token */
    long getTokenStartPos ();

    /** Get the end position of the current token */
    long getTokenEndPos ();

    /** Get the line number of the current token */
    long getLineNumber ();

    /** Get the start column of the current token */
    long getTokenColumn ();

    /** Check if there are any more tokens */
    boolean hasMoreTokens ();

    /** Get the next token */
    Token nextToken ();

    /** Get the next non-whitespace token.
     *  If there are no more non-whitespace tokens, then END_OF_INPUT will be returned
     */
    default Token nextNonWhitespaceToken () {
	while (hasMoreTokens ()) {
	    Token t = nextToken ();
	    if (!t.isWhitespace ())
		return t;
	}
	return Token.END_OF_INPUT;
    }

    /** Make several consecutive &gt; be reported individually so that
     *  generic types can be correctly parsed.
     */
    void setInsideTypeContext (boolean insideTypeContext);
}