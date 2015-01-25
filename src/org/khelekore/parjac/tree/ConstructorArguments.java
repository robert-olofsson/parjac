package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ConstructorArguments implements TreeNode {
    private final ArgumentList args;

    public ConstructorArguments (Rule r, Deque<TreeNode> parts) {
	args = r.size () > 3 ? (ArgumentList)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{(" + args + ");}";
    }
}
