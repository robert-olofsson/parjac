package org.khelekore.parjac.tree;

import java.util.Deque;

public class PreIncrementExpression implements TreeNode {
    private final TreeNode exp;

    public PreIncrementExpression (Deque<TreeNode> parts) {
	parts.pop (); // '++'
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }
}
