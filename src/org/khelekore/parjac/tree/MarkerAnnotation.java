package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class MarkerAnnotation extends NamedNode {
    public MarkerAnnotation (DottedName name, ParsePosition ppos) {
	super (name, ppos);
    }

    public MarkerAnnotation (Deque<TreeNode> parts, ParsePosition ppos) {
	super (parts, ppos);
    }
}