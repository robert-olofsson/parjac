package org.khelekore.parjac.tree;

public class Identifier implements TreeNode {
    private final String value;

    public Identifier (String value) {
	this.value = value;
    }

    public String get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}