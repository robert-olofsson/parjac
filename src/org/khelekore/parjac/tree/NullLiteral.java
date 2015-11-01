package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class NullLiteral extends PositionNode {

    public NullLiteral (ParsePosition pos) {
	super (pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public ExpressionType getExpressionType () {
	return ExpressionType.NULL;
    }
}