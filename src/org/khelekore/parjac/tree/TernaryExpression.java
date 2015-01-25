package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class TernaryExpression implements TreeNode {
    private final TreeNode test;
    private final TreeNode thenPart;
    private final TreeNode elsePart;

    public TernaryExpression (TreeNode test, TreeNode thenPart, TreeNode elsePart) {
	this.test = test;
	this.thenPart = thenPart;
	this.elsePart = elsePart;
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    return parts.pop ();
	TreeNode test = parts.pop ();
	parts.pop (); // '?'
	TreeNode thenPart = parts.pop ();
	parts.pop (); // ':'
	TreeNode elsePart = parts.pop ();
	return new TernaryExpression (test, thenPart, elsePart);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + test + " ? " + thenPart + " : " + elsePart + "}";
    }
}
