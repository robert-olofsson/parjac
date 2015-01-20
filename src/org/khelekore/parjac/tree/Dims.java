package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class Dims implements TreeNode {
    private final List<List<TreeNode>> annotations;

    public Dims (Rule r, Deque<TreeNode> parts) {
	// TODO: implement correctly
	annotations = Collections.emptyList ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations + "}";
    }
}