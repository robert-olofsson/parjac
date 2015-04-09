package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class LambdaExpression implements TreeNode {
    private final TreeNode parameters;
    private final TreeNode body;

    public LambdaExpression (Deque<TreeNode> parts, ParsePosition ppos) {
	parameters = parts.pop ();
	parts.pop (); // '->'
	body = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + parameters + " -> " + body + "}";
    }
}
