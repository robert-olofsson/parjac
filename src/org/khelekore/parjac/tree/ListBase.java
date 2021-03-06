package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.ZOMEntry;

public abstract class ListBase<T extends TreeNode> extends PositionNode {
    private final List<T> ls;

    public ListBase (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	@SuppressWarnings("unchecked")T vd = (T)parts.pop ();
	if (r.size () == 1) {
	    ls = new ArrayList<> (1);
	    ls.add (vd);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    ls = ze.get ();
	    ls.add (0, vd);
	}
    }

    public ListBase (List<T> ls, ParsePosition pos) {
	super (pos);
	this.ls = ls;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{ls: " + ls + "}";
    }

    public List<T> get () {
	return ls;
    }

    @Override public void visit (TreeVisitor visitor) {
	ls.forEach (t -> t.visit (visitor));
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	return ls;
    }

    public int size () {
	return ls.size ();
    }
}