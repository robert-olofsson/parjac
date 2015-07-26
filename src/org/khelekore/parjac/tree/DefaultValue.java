package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class DefaultValue extends PositionNode {
    private final TreeNode value;

    public DefaultValue (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	parts.pop (); // 'default'
	this.value = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{default " + value + "}";
    }
}
