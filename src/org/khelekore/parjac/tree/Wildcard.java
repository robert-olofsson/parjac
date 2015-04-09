package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class Wildcard implements TreeNode {
    private final List<TreeNode> annotations;
    private final WildcardBounds bounds;

    public Wildcard (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	int len = 1;
	if (r.getRulePart (0).getId () != Token.QUESTIONMARK) {
	    annotations = ((ZOMEntry)parts.pop ()).get ();
	    len++;
	} else {
	    annotations = null;
	}
	parts.pop (); // '?'
	bounds = r.size () > len ? (WildcardBounds)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + " ? " + bounds + "}";
    }
}
