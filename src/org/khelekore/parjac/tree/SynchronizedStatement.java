package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class SynchronizedStatement extends PositionNode {
    private final TreeNode expression;
    private final Block block;

    public SynchronizedStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	parts.pop (); // 'synchronized'
	expression = parts.pop ();
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + expression + " " + block + "}";
    }
}
