package org.khelekore.parjac.tree;

import java.util.Deque;

public class ExtendsInterfaces implements TreeNode {
    private final InterfaceTypeList ls;

    public ExtendsInterfaces (Deque<TreeNode> parts) {
	ls = (InterfaceTypeList)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{extends " + ls + "}";
    }
}
