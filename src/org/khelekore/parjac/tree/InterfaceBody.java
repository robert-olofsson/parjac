package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class InterfaceBody extends BodyBase {
    public InterfaceBody (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}
