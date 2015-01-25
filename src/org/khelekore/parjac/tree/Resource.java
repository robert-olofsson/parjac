package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class Resource implements TreeNode {
    private final List<TreeNode> modifiers;
    private final TreeNode type;
    private final VariableDeclaratorId vdi;
    private final TreeNode exp;

    public Resource (Rule r, Deque<TreeNode> parts) {
	modifiers = r.size () > 4 ? ((ZOMEntry)parts.pop ()).get () : null;
	type = parts.pop ();
	vdi = (VariableDeclaratorId)parts.pop ();
	parts.pop (); // '='
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + type + " " + vdi + " = " + exp + "}";
    }
}
