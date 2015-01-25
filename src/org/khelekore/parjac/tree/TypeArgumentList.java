package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class TypeArgumentList implements TreeNode {
    private final List<TreeNode> ls;

    public TypeArgumentList (Rule r, Deque<TreeNode> parts) {
	TreeNode ta = parts.pop ();
	if (r.size () == 1) {
	    ls = Collections.singletonList (ta);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    ls = ze.get ();
	    ls.add (0, ta);
	}
    }

    public List<TreeNode> get () {
	return ls;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ls + "}";
    }
}
