package org.khelekore.parjac.tree;

public class CharLiteral implements TreeNode {
    private final char value;

    public CharLiteral (char value) {
	this.value = value;
    }

    public char get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}