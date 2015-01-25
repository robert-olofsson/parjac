package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ArgumentList extends ListBase<TreeNode>  {
    public ArgumentList (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}