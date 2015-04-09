package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ExpressionStatement implements TreeNode {
    private final TreeNode exp;

    public ExpressionStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + ";}";
    }

    public void visit (TreeVisitor visitor) {
	exp.visit (visitor);
    }
}
