package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class AnnotationTypeBody extends BodyBase {
    public AnnotationTypeBody (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}
