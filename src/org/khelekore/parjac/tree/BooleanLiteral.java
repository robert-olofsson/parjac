package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class BooleanLiteral extends PositionNode implements LiteralValue {
    private final boolean value;

    public BooleanLiteral (boolean value, ParsePosition pos) {
	super (pos);
	this.value = value;
    }

    public boolean get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public ExpressionType getExpressionType () {
	return ExpressionType.BOOLEAN;
    }

    public static boolean isTrue (TreeNode t) {
	if (t instanceof BooleanLiteral)
	    return ((BooleanLiteral)t).get ();
	return false;
    }
}