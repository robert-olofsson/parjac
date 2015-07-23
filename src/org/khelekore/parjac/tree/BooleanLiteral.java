package org.khelekore.parjac.tree;

public class BooleanLiteral implements TreeNode {
    private final boolean value;

    public static final BooleanLiteral TRUE_VALUE = new BooleanLiteral (true);
    public static final BooleanLiteral FALSE_VALUE = new BooleanLiteral (false);

    private BooleanLiteral (boolean value) {
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
}