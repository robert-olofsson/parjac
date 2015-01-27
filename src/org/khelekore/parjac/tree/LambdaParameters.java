package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class LambdaParameters implements TreeNode {
    private final TreeNode parameters;

    public LambdaParameters (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    parameters = parts.pop ();
	else if (r.size () == 2)
	    parameters = null;
	else
	    parameters = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + parameters + "}";
    }
}
