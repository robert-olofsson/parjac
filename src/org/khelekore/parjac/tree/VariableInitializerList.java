package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class VariableInitializerList extends ListBase<TreeNode>  {
    public VariableInitializerList (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (r, parts, ppos);
    }
}