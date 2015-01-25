package org.khelekore.parjac.tree;

import java.util.Deque;

public class PostDecrementExpression implements TreeNode {
    private final TreeNode exp;

    public PostDecrementExpression (Deque<TreeNode> parts) {
	exp = parts.pop ();
	parts.pop (); // '--'
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }
}
