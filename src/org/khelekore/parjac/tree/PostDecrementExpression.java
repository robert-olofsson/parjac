package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class PostDecrementExpression extends PositionNode {
    private final TreeNode exp;

    public PostDecrementExpression (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	exp = parts.pop ();
	parts.pop (); // '--'
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }
}
