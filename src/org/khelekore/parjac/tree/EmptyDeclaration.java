package org.khelekore.parjac.tree;

import java.util.Deque;

public class EmptyDeclaration implements TreeNode {
    public static final EmptyDeclaration INSTANCE = new EmptyDeclaration ();

    private EmptyDeclaration () {
    }

    public static EmptyDeclaration build (Deque<TreeNode> parts) {
	return INSTANCE;
    }

    @Override public String toString () {
	return getClass ().getSimpleName ();
    }
}