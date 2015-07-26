package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class InferredFormalParameterList extends PositionNode {
    private final List<Identifier> ids;

    public InferredFormalParameterList (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	Identifier i = (Identifier)parts.pop ();
	if (r.size () == 1) {
	    ids = Collections.singletonList (i);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    ids = ze.get ();
	    ids.add (0, i);
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ids + "}";
    }
}
