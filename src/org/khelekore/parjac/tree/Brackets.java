package org.khelekore.parjac.tree;

import java.util.Deque;

public class Brackets implements TreeNode {
    public static final Brackets INSTANCE = new Brackets ();

    private Brackets () {
    }

    public static Brackets build (Deque<TreeNode> parts) {
	return INSTANCE;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }
}