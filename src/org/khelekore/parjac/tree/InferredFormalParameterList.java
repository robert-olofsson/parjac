package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class InferredFormalParameterList implements TreeNode {
    private final List<Identifier> ids;

    public InferredFormalParameterList (Rule r, Deque<TreeNode> parts) {
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
