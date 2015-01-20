package org.khelekore.parjac.tree;

import java.util.Deque;

public class SingleElementAnnotation extends NamedNode {
    private TreeNode value;

    public SingleElementAnnotation (Deque<TreeNode> parts) {
	super (parts);
	value = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + getName () +
	    ", value: " + value + "}";
    }
}