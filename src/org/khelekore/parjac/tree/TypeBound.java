package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class TypeBound implements TreeNode {
    private final ClassType ct;
    private final List<AdditionalBound> additionalBounds;

    public TypeBound (Rule r, Deque<TreeNode> parts) {
	ct = (ClassType)parts.pop ();
	additionalBounds = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ct + " " + additionalBounds + "}";
    }
}
