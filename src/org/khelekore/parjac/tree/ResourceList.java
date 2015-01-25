package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ResourceList extends ListBase<Resource>  {
    public ResourceList (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}