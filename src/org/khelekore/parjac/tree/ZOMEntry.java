package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZOMEntry implements TreeNode {
    private List<TreeNode> nodes = new ArrayList<> ();

    public ZOMEntry () {
    }

    public void add (TreeNode node) {
	nodes.add (node);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{nodes: " + nodes + "}";
    }

    public List<TreeNode> getNodes () {
	return Collections.unmodifiableList (nodes);
    }
}