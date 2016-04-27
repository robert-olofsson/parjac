package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class OneDim extends PositionNode {
    private final List<TreeNode> annotations;

    public OneDim (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	annotations = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
    }

    public OneDim () {
	super (null);
	annotations = null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + "}";
    }
}
