package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class ExtraName implements TreeNode {
    private final List<TreeNode> annotations;
    private final String id;

    public ExtraName (Rule r, Deque<TreeNode> parts) {
	annotations = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	id = ((Identifier)parts.pop ()).get ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{." + annotations + " " + id + "}";
    }
}
