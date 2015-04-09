package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ReturnStatement implements TreeNode {
    private final TreeNode exp;

    public ReturnStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	exp = r.size () > 2 ? parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (exp != null)
	    exp.visit (visitor);
    }
}