package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class DoStatement implements TreeNode {
    private final TreeNode statement;
    private final TreeNode exp;

    public DoStatement (Rule r, Deque<TreeNode> parts) {
	statement = parts.pop ();
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + statement + " " + exp + "}";
    }
}
