package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class Dims extends Multipart<OneDim> {
    public Dims (OneDim od) {
	super (od);
    }

    public static Dims build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 1)
	    return new Dims ((OneDim)parts.pop ());
	Dims d = (Dims)parts.pop ();
	d.add ((OneDim)parts.pop ());
	return d;
    }
}