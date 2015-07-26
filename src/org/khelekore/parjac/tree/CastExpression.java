package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class CastExpression extends PositionNode {
    private final TreeNode type;
    private final List<TreeNode> additionalBounds;
    private final TreeNode expression;

    public CastExpression (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	type = parts.pop ();
	additionalBounds = r.size () > 4 ? ((ZOMEntry)parts.pop ()).get () : null;
	expression = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + additionalBounds + " " + expression + "}";
    }
}
