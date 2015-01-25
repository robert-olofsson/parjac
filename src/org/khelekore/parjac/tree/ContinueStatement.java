package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ContinueStatement implements TreeNode {
    private final String id;

    public ContinueStatement (Rule r, Deque<TreeNode> parts) {
	id = r.size () > 2 ? ((Identifier)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + "}";
    }
}
