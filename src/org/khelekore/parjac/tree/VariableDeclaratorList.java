package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.tree.VariableDeclarator;

public class VariableDeclaratorList extends ListBase<VariableDeclarator>  {
    public VariableDeclaratorList (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}