package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class DimExprs extends ListBase<DimExpr>  {
    public DimExprs (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}