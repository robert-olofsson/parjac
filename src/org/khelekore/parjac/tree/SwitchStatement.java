package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class SwitchStatement implements TreeNode {
    private final TreeNode exp;
    private final SwitchBlock block;

    public SwitchStatement (Rule r, Deque<TreeNode> parts) {
	exp = parts.pop ();
	block = (SwitchBlock)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{switch (" + exp + ")" + block + "}";
    }
}
