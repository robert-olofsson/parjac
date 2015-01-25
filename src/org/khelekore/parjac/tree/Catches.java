package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class Catches extends ListBase<CatchClause>  {
    public Catches (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}