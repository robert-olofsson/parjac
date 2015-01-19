package org.khelekore.parjac.tree;

public class BooleanValue implements TreeNode {
    private final boolean value;

    public static final BooleanValue TRUE_VALUE = new BooleanValue (true);
    public static final BooleanValue FALSE_VALUE = new BooleanValue (false);

    private BooleanValue (boolean value) {
	this.value = value;
    }

    public boolean get () {
	return value;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + value + "}";
    }
}