package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ElementValuePair implements TreeNode  {
    private final String id;
    private final TreeNode value;

    public ElementValuePair (Rule r, Deque<TreeNode> parts) {
	id = ((Identifier)parts.pop ()).get ();
	parts.pop (); // '='
	value = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + " = " + value + "}";
    }
}