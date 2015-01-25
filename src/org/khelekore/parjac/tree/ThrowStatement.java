package org.khelekore.parjac.tree;

import java.util.Deque;

public class ThrowStatement implements TreeNode {
    private final TreeNode exp;

    public ThrowStatement (Deque<TreeNode> parts) {
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }
}
