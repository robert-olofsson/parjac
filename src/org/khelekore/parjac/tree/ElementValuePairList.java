package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ElementValuePairList extends ListBase<ElementValuePair>  {
    public ElementValuePairList (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}