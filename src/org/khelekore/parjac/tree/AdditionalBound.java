package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class AdditionalBound extends PositionNode {
    private final ClassType ct;

    public AdditionalBound (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	parts.pop (); // '&'
	ct = (ClassType)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ct + "}";
    }

    public ClassType getType () {
	return ct;
    }
}
