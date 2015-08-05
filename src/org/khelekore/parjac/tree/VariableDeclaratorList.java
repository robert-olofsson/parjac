package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.VariableDeclarator;

public class VariableDeclaratorList extends ListBase<VariableDeclarator>  {
    public VariableDeclaratorList (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (r, parts, pos);
    }

    public VariableDeclaratorList (List<VariableDeclarator> ls, ParsePosition pos) {
	super (ls, pos);
    }
}