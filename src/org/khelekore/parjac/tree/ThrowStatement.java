package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class ThrowStatement extends PositionNode {
    private final TreeNode exp;

    public ThrowStatement (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }
}
