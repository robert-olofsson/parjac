package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class PrimitiveType implements TreeNode {
    private final List<TreeNode> annotations;
    private final TreeNode type;

    public PrimitiveType (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	annotations = r.size () > 1 ? ((ZOMEntry)parts.pop ()).get () : null;
	type = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations +
	    ", type: " + type + "}";
    }
}