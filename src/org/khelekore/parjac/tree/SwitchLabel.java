package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class SwitchLabel implements TreeNode {
    private final TreeNode constantExp;

    public SwitchLabel (TreeNode constantExp) {
	this.constantExp = constantExp;
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    return DefaultLabel.INSTANCE;
	TreeNode constantExp = parts.pop ();
	parts.pop (); // ':'
	return new SwitchLabel (constantExp);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + constantExp + "}";
    }
}
