package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class OperatorTokenType extends TokenTypeBase {
    public OperatorTokenType (Token type, ParsePosition pos) {
	super (type, pos);
    }
}