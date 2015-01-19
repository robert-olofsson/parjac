package org.khelekore.parjac.tree;

import java.util.Deque;

public class NamedNode implements TreeNode {
    private final DottedName name;

    public NamedNode (Deque<TreeNode> parts) {
	this.name = (DottedName)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + name + "}";
    }

    public DottedName getName () {
	return name;
    }
}