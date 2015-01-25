package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class TryStatement implements TreeNode {
    private final ResourceList resources;
    private final Block block;
    private final Catches catches;
    private final Finally finallyBlock;

    public TryStatement (Rule r, Deque<TreeNode> parts) {
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
}
