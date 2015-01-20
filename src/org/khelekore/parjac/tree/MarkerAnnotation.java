package org.khelekore.parjac.tree;

import java.util.Deque;

public class MarkerAnnotation extends NamedNode {
    public MarkerAnnotation (DottedName name) {
	super (name);
    }

    public MarkerAnnotation (Deque<TreeNode> parts) {
	super (parts);
    }
}