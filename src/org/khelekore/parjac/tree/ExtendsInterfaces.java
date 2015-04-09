package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class ExtendsInterfaces implements TreeNode {
    private final InterfaceTypeList ls;

    public ExtendsInterfaces (Deque<TreeNode> parts, ParsePosition ppos) {
	ls = (InterfaceTypeList)parts.pop ();
    }

    public InterfaceTypeList get () {
	return ls;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{extends " + ls + "}";
    }
}
