package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class PrimitiveTokenType extends TokenTypeBase {
    public PrimitiveTokenType (Token type, ParsePosition pos) {
	super (type, pos);
    }

    @Override public String getExpressionType () {
	switch (get ()) {
	case BYTE:
	    return "B";
	case SHORT:
	    return "S";
	case INT:
	    return "I";
	case LONG:
	    return "J";
	case CHAR:
	    return "C";
	case FLOAT:
	    return "F";
	case DOUBLE:
	    return "D";
	case BOOLEAN:
	    return "Z";
	default:
	    return null;
	}
    }
}