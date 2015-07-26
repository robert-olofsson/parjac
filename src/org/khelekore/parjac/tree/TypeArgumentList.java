package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class TypeArgumentList extends PositionNode {
    private final List<TreeNode> ls;

    public TypeArgumentList (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
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
