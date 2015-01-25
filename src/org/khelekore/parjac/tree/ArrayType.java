package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ArrayType implements TreeNode {
    private final TreeNode type;
    private final Dims dims;

    public ArrayType (Rule r, Deque<TreeNode> parts) {
	type = parts.pop ();
	dims = (Dims)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + dims + "}";
    }
}
