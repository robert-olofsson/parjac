package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class SingleElementAnnotation extends NamedNode {
    private TreeNode value;

    public SingleElementAnnotation (Deque<TreeNode> parts, ParsePosition ppos) {
	super (parts, ppos);
	value = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + getName () +
	    ", value: " + value + "}";
    }
}