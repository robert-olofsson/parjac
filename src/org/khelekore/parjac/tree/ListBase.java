package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.ZOMEntry;

public abstract class ListBase<T extends TreeNode> implements TreeNode {
    private final List<T> ls;

    public ListBase (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	@SuppressWarnings("unchecked")T vd = (T)parts.pop ();
	if (r.size () == 1) {
	    ls = Collections.singletonList (vd);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    ls = ze.get ();
	    ls.add (0, vd);
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{ls: " + ls + "}";
    }

    public List<T> get () {
	return ls;
    }

    public void visit (TreeVisitor visitor) {
	ls.forEach (t -> t.visit (visitor));
    }
}