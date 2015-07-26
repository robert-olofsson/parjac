package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class Diamond extends PositionNode {
    public Diamond (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	parts.pop (); // '<'
	parts.pop (); // '>'
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }
}