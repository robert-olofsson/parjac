package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class Finally extends PositionNode {
    private final Block block;

    public Finally (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + block + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	block.visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (block);
    }

    public Block getBlock () {
	return block;
    }
}
