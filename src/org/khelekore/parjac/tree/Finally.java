package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class Finally implements TreeNode {
    private final Block block;

    public Finally (Deque<TreeNode> parts, ParsePosition ppos) {
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + block + "}";
    }
}
