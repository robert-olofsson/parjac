package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class AdditionalBound implements TreeNode {
    private final ClassType ct;

    public AdditionalBound (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	parts.pop (); // '&'
	ct = (ClassType)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ct + "}";
    }
}
