package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class ReceiverParameter implements TreeNode {
    private final List<TreeNode> annotations;
    private final TreeNode type;
    private final List<String> ids;

    public ReceiverParameter (Rule r, Deque<TreeNode> parts) {
	int len = 2;
	TreeNode tn = parts.pop ();
	if (tn instanceof ZOMEntry) {
	    annotations = ((ZOMEntry)tn).get ();
	    tn = parts.pop ();
	    len++;
	} else {
	    annotations = null;
	}
	type = tn;
	if (r.size () > len) {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    ids = new ArrayList<> (ze.size ());
	    for (Identifier i : ze.<Identifier>get ())
		ids.add (i.get ());
	} else {
	    ids = null;
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + " " + type + " " + ids + " this}";
    }
}
