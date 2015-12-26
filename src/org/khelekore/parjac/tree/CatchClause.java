package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class CatchClause extends PositionNode {
    private final CatchFormalParameter cfp;
    private final Block block;

    public CatchClause (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	cfp = (CatchFormalParameter)parts.pop ();
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + cfp + " " + block + "}";
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
