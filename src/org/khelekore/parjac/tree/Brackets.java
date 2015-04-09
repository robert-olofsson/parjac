package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class Brackets implements TreeNode {
    public static final Brackets INSTANCE = new Brackets ();

    private Brackets () {
    }

    public static Brackets build (Deque<TreeNode> parts, ParsePosition ppos) {
	return INSTANCE;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }
}