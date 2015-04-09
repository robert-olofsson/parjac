package org.khelekore.parjac.lexer;

public class ParsePosition {
    private int lineNumber;
    private int tokenColumn;
    private int tokenStartPos;
    private int tokenEndPos;

    public ParsePosition (int lineNumber, int tokenColumn,
			  int tokenStartPos, int tokenEndPos) {
	this.lineNumber = lineNumber;
	this.tokenColumn = tokenColumn;
	this.tokenStartPos = tokenStartPos;
	this.tokenEndPos = tokenEndPos;
    }

    /** Get the start position of the current token */
    public int getTokenStartPos () {
	return tokenStartPos;
    }

    /** Get the end position of the current token */
    public int getTokenEndPos () {
	return tokenEndPos;
    }

    /** Get the line number of the current token */
    public int getLineNumber () {
	return lineNumber;
    }

    /** Get the start column of the current token */
    public int getTokenColumn () {
	return tokenColumn;
    }
}