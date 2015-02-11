package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class LastFormalParameter implements TreeNode {
    private final List<TreeNode> modifiers;
    private final TreeNode type;
    private final List<TreeNode> annotations;
    private final VariableDeclaratorId vdi;

    public LastFormalParameter (Rule r, Deque<TreeNode> parts) {
	int len = 3;
	TreeNode tn = parts.pop ();
	if (tn instanceof ZOMEntry) {
	    len++;
	    modifiers = ((ZOMEntry)tn).get ();
	    tn = parts.pop ();
	} else {
	    modifiers = null;
	}
	type = tn;
	annotations = r.size () > len ? ((ZOMEntry)parts.pop ()).get () : null;
	vdi = (VariableDeclaratorId)parts.pop ();
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    return parts.pop ();
	return new LastFormalParameter (r, parts);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " +
	    type + " " + annotations + " " + vdi + "}";
    }

    public TreeNode getType () {
	return type;
    }
}
