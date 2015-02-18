package org.khelekore.parjac.tree;

import java.util.Deque;

public class UnannArrayType implements TreeNode {
    private final TreeNode type;
    private final Dims dims;

    public UnannArrayType (Deque<TreeNode> parts) {
	type = parts.pop ();
	dims = (Dims)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + dims + "}";
    }

    public TreeNode getType () {
	return type;
    }

    public Dims getDims () {
	return dims;
    }
}
