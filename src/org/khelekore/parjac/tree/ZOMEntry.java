package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZOMEntry implements TreeNode {
    private final List<TreeNode> nodes = new ArrayList<> ();

    public ZOMEntry () {
    }

    public void add (TreeNode node) {
	nodes.add (node);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{nodes: " + nodes + "}";
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> get () {
	return (List<T>)nodes;
    }

    public int size () {
	return nodes.size ();
    }
}