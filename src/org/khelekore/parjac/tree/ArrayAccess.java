package org.khelekore.parjac.tree;

import java.util.Deque;

public class ArrayAccess implements TreeNode {
    private final TreeNode from;
    private final TreeNode exp;

    public ArrayAccess (Deque<TreeNode> parts) {
	from = parts.pop ();
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + from + "[" + exp + "]" + "}";
    }
}
