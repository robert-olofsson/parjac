package org.khelekore.parjac.tree;

import java.util.Deque;

public class PostIncrementExpression implements TreeNode {
    private final TreeNode exp;

    public PostIncrementExpression (Deque<TreeNode> parts) {
	exp = parts.pop ();
	parts.pop (); // '++'
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }
}
