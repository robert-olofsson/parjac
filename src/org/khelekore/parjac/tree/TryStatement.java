package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class TryStatement extends PositionNode {
    private final ResourceList resources;
    private final Block block;
    private final Catches catches;
    private final Finally finallyBlock;

    public TryStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
	int pos = 1;
	if (r.size () > 4) {
	    resources = (ResourceList)parts.pop ();
	    pos = 4;
	    if (r.getRulePart (pos).isTokenPart ())
		pos++;
	} else {
	    resources = null;
	}
	block = (Block)parts.pop ();
	pos++;
	if (r.size () > pos) {
	    if (r.getRulePart (pos).getId ().equals ("Catches")) {
		catches = (Catches)parts.pop ();
		pos++;
	    } else {
		catches = null;
	    }
	    finallyBlock = r.size () > pos ? (Finally)parts.pop () : null;
	} else {
	    catches = null;
	    finallyBlock = null;
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + resources + " " +
	    block + " " + catches + " " + finallyBlock + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    if (resources != null)
		resources.visit (visitor);
	    block.visit (visitor);
	    if (catches != null)
		catches.visit (visitor);
	    if (finallyBlock != null)
		finallyBlock.visit (visitor);
	}
    }

    public Collection<? extends TreeNode> getChildNodes () {
	List<TreeNode> ls = new ArrayList<> ();
	if (resources != null)
	    ls.add (resources);
	ls.add (block);
	if (catches != null)
	    ls.add (catches);
	if (finallyBlock != null)
	    ls.add (finallyBlock);
	return ls;
    }

    public ResourceList getResources () {
	return resources;
    }

    public Block getBlock () {
	return block;
    }

    public Catches getCatches () {
	return catches;
    }

    public Finally getFinallyBlock () {
	return finallyBlock;
    }
}
