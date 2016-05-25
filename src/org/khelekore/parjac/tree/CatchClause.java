package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

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
	if (visitor.visit (this)) {
	    cfp.visit (visitor);
	    block.visit (visitor);
	}
	visitor.endCatchClause (this);
    }

    public CatchFormalParameter getFormalParameter () {
	return cfp;
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (block);
    }

    public List<ClassType> getTypes () {
	return cfp.getTypes ();
    }

    public Block getBlock () {
	return block;
    }
}
