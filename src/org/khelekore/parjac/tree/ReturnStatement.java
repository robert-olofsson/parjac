package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ReturnStatement implements TreeNode {
    private final TreeNode exp;

    public ReturnStatement (Rule r, Deque<TreeNode> parts) {
	exp = r.size () > 2 ? parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	exp.visit (visitor);
    }
}