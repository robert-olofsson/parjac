package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class EmptyStatement implements TreeNode {
    public static final EmptyStatement INSTANCE = new EmptyStatement ();

    private EmptyStatement () {
    }

    public static EmptyStatement build (Deque<TreeNode> parts, ParsePosition ppos) {
	return INSTANCE;
    }

    @Override public String toString () {
	return getClass ().getSimpleName ();
    }
}