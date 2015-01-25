package org.khelekore.parjac.tree;

import java.util.Deque;

public class Diamond implements TreeNode {
    public static final Diamond INSTANCE = new Diamond ();

    private Diamond () {
    }

    public static Diamond build (Deque<TreeNode> parts) {
	parts.pop (); // '<'
	parts.pop (); // '>'
	return INSTANCE;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }
}