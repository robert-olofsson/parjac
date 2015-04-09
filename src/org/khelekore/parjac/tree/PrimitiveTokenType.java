package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class PrimitiveTokenType extends TokenTypeBase {
    public PrimitiveTokenType (Token type, ParsePosition pos) {
	super (type, pos);
    }
}