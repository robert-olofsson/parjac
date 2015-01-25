package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ExpressionStatement implements TreeNode {
    private final TreeNode exp;

    public ExpressionStatement (Rule r, Deque<TreeNode> parts) {
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + ";}";
    }
}
