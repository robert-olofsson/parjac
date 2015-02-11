package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class FormalParameter implements TreeNode {
    private final List<TreeNode> annotations;
    private final TreeNode type;
    private final VariableDeclaratorId vdi;

    public FormalParameter (Rule r, Deque<TreeNode> parts) {
	annotations = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	type = parts.pop ();
	vdi = (VariableDeclaratorId)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + " " + type + " " + vdi + "}";
    }

    public TreeNode getType () {
	return type;
    }
}
