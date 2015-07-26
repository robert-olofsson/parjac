package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class ExtendsInterfaces extends PositionNode {
    private final InterfaceTypeList ls;

    public ExtendsInterfaces (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	ls = (InterfaceTypeList)parts.pop ();
    }

    public InterfaceTypeList get () {
	return ls;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{extends " + ls + "}";
    }
}
