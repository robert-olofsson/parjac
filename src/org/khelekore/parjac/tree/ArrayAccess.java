package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class ArrayAccess extends PositionNode {
    private final TreeNode from;
    private final TreeNode exp;

    public ArrayAccess (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	from = parts.pop ();
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + from + "[" + exp + "]" + "}";
    }
}
