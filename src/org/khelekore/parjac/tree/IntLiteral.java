package org.khelekore.parjac.tree;

public class IntLiteral implements TreeNode {
    private final int value;

    public IntLiteral (int value) {
	this.value = value;
    }

    public int get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}