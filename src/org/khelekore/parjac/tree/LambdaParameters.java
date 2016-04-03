package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class LambdaParameters extends PositionNode {
    private final TreeNode parameters;

    public LambdaParameters (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	// o -> ..., () -> ..., (<inferred|formal>) -> ...
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

    public TreeNode getParameters () {
	return parameters;
    }
}
