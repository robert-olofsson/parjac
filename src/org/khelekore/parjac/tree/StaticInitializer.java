package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class StaticInitializer implements TreeNode {
    private final Block block;

    public StaticInitializer (Deque<TreeNode> parts) {
	parts.pop (); // 'static'
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + block + "}";
    }
}
