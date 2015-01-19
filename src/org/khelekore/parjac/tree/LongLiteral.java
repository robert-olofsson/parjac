package org.khelekore.parjac.tree;

public class LongLiteral implements TreeNode {
    private final long value;

    public LongLiteral (long value) {
	this.value = value;
    }

    public long get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}