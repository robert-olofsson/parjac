package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class InterfaceTypeList extends ListBase<ClassType>  {
    public InterfaceTypeList (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}