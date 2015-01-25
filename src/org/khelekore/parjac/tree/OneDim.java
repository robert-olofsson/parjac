package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class OneDim implements TreeNode {
    private final List<TreeNode> annotations;

    public OneDim (Rule r, Deque<TreeNode> parts) {
	annotations = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + "}";
    }
}
