package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class InterfaceBody implements TreeNode {
    private final List<TreeNode> members;

    public InterfaceBody (Rule r, Deque<TreeNode> parts) {
	members = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + members + "}";
    }
}
