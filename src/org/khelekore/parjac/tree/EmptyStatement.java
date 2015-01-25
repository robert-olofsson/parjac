package org.khelekore.parjac.tree;

import java.util.Deque;

public class EmptyStatement implements TreeNode {
    public static final EmptyStatement INSTANCE = new EmptyStatement ();

    private EmptyStatement () {
    }

    public static EmptyStatement build (Deque<TreeNode> parts) {
	return INSTANCE;
    }

    @Override public String toString () {
	return getClass ().getSimpleName ();
    }
}