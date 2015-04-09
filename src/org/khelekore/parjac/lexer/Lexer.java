package org.khelekore.parjac.lexer;

import org.khelekore.parjac.lexer.ParsePosition;

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

    /** Get the current identifier */
    String getIdentifier ();

    /** Get the current parse position */
    ParsePosition getParsePosition ();

    /** Check if there are any more tokens.
     *  Note that this method will return true until END_OF_INPUT has been returned.
     */
    boolean hasMoreTokens ();

    /** Get the next token */
    Token nextToken ();

    /** Get the current line */
    String getCurrentLine ();

    /** Get the next non-whitespace token.
     *  If there are no more non-whitespace tokens, then END_OF_INPUT will be returned
     */
    Token nextNonWhitespaceToken ();
}