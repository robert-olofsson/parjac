package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class VariableDeclaratorId implements TreeNode {
    private final String id;
    private final Dims dims;

    public VariableDeclaratorId (Rule r, Deque<TreeNode> parts) {
	id = ((Identifier)parts.pop ()).get ();
	dims = r.size () > 1 ? (Dims)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{id: " + id + ", dims: " + dims + "}";
    }

    public String getId () {
	return id;
    }
}