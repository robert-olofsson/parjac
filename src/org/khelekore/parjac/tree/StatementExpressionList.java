package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class StatementExpressionList extends ListBase<TreeNode>  {
    public StatementExpressionList (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}