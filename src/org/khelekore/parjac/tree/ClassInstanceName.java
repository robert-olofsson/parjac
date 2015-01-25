package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class ClassInstanceName implements TreeNode {
    private final String id;
    private final List<ExtraName> extras;

    public ClassInstanceName (Rule r, Deque<TreeNode> parts) {
	id = ((Identifier)parts.pop ()).get ();
	extras = r.size () > 1 ? ((ZOMEntry)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + " " + extras + "}";
    }
}
