package org.khelekore.parjac.tree;

public class FloatLiteral implements TreeNode {
    private final float value;

    public FloatLiteral (float value) {
	this.value = value;
    }

    public float get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}