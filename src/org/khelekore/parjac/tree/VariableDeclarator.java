package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class VariableDeclarator implements TreeNode {
    private final VariableDeclaratorId vdi;

    public VariableDeclarator (Rule r, Deque<TreeNode> parts) {
	vdi = (VariableDeclaratorId)parts.pop ();
	if (r.size () > 1) {
	    parts.pop (); // '='
	    parts.pop (); // TODO: handle VariableInitializer
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{vdi: " + vdi + "}";
    }
}