package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class DoStatement implements TreeNode {
    private final TreeNode statement;
    private final TreeNode exp;

    public DoStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	statement = parts.pop ();
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + statement + " " + exp + "}";
    }
}
