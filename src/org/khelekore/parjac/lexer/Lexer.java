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

    /** Get the current identifier */
    String getIdentifier ();

    /** Get the start position of the current token */
    long getTokenStartPos ();

    /** Get the end position of the current token */
    long getTokenEndPos ();

    /** Get the line number of the current token */
    long getLineNumber ();

    /** Get the start column of the current token */
    long getTokenColumn ();

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

    /** Make several consecutive &gt; be reported individually so that
     *  generic types can be correctly parsed.
     */
    void pushInsideTypeContext ();

    /** If all pushed inside type context have been popped conecutive &gt;
     *  will be reported as shift.
     */
    void popInsideTypeContext ();

    /** Check if the lexer is currently inside a type context. */
    boolean isInsideTypeContext ();

}