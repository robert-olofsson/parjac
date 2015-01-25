package org.khelekore.parjac.tree;

import java.util.Deque;

public class Finally implements TreeNode {
    private final Block block;

    public Finally (Deque<TreeNode> parts) {
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + block + "}";
    }
}
