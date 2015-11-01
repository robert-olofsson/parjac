package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class PrimitiveTokenType extends TokenTypeBase {
    public PrimitiveTokenType (Token type, ParsePosition pos) {
	super (type, pos);
    }

    @Override public ExpressionType getExpressionType () {
	switch (get ()) {
	case BYTE:
	    return ExpressionType.BYTE;
	case SHORT:
	    return ExpressionType.SHORT;
	case INT:
	    return ExpressionType.INT;
	case LONG:
	    return ExpressionType.LONG;
	case CHAR:
	    return ExpressionType.CHAR;
	case FLOAT:
	    return ExpressionType.FLOAT;
	case DOUBLE:
	    return ExpressionType.DOUBLE;
	case BOOLEAN:
	    return ExpressionType.BOOLEAN;
	default:
	    return null;
	}
    }
}