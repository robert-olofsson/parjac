package org.khelekore.parjac.tree;

public class DefaultLabel implements TreeNode {
    public static final DefaultLabel INSTANCE = new DefaultLabel ();

    private DefaultLabel () {
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }
}