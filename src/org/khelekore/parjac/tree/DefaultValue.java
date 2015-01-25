package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class DefaultValue implements TreeNode {
    private final TreeNode value;

    public DefaultValue (Deque<TreeNode> parts) {
	this.value = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{default " + value + "}";
    }
}
