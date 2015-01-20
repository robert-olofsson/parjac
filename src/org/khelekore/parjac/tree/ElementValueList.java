package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ElementValueList extends ListBase<TreeNode>  {
    public ElementValueList (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}