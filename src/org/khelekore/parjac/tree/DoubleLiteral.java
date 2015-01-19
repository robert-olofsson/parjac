package org.khelekore.parjac.tree;

public class DoubleLiteral implements TreeNode {
    private final double value;

    public DoubleLiteral (double value) {
	this.value = value;
    }

    public double get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}