package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class CastExpression implements TreeNode {
    private final TreeNode type;
    private final List<TreeNode> additionalBounds;
    private final TreeNode expression;

    public CastExpression (Rule r, Deque<TreeNode> parts) {
	type = parts.pop ();
	additionalBounds = r.size () > 4 ? ((ZOMEntry)parts.pop ()).get () : null;
	expression = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + additionalBounds + " " + expression + "}";
    }
}
