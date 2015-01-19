package org.khelekore.parjac.tree;

public class StringLiteral implements TreeNode {
    private final String value;

    public StringLiteral (String value) {
	this.value = value;
    }

    public String get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}