package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class VariableDeclarator implements TreeNode {
    private final VariableDeclaratorId vdi;
    private final TreeNode initializer;

    public VariableDeclarator (Rule r, Deque<TreeNode> parts) {
	vdi = (VariableDeclaratorId)parts.pop ();
	if (r.size () > 1) {
	    parts.pop (); // '='
	    initializer = parts.pop ();
	} else {
	    initializer = null;
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{vdi: " + vdi + " = " + initializer + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (initializer != null)
	    initializer.visit (visitor);
    }
}