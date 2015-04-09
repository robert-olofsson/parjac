package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class InterfaceTypeList extends ListBase<ClassType>  {
    public InterfaceTypeList (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (r, parts, pos);
    }
}