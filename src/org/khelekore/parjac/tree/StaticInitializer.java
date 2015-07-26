package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class StaticInitializer extends PositionNode {
    private final Block block;

    public StaticInitializer (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	parts.pop (); // 'static'
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + block + "}";
    }
}
