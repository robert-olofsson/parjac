package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class InstanceInitializer implements TreeNode {
    private final Block block;

    public InstanceInitializer (Deque<TreeNode> parts) {
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + block + "}";
    }
}
