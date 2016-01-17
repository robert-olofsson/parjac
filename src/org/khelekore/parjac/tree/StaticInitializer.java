package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
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

    /** Visit this node and its child nodes */
    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    block.visit (visitor);
    }

    /** Visit this node */
    @Override public void simpleVisit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (block);
    }
}
