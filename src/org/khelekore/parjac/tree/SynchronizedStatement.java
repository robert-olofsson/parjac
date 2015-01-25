package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class SynchronizedStatement implements TreeNode {
    private final TreeNode expression;
    private final Block block;

    public SynchronizedStatement (Rule r, Deque<TreeNode> parts) {
	parts.pop (); // 'synchronized'
	expression = parts.pop ();
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + expression + " " + block + "}";
    }
}
