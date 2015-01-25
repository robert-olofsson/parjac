package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class IfThenStatement implements TreeNode {
    private final TreeNode exp;
    private final TreeNode ifStatement;
    private final TreeNode elseStatement;

    public IfThenStatement (Rule r, Deque<TreeNode> parts) {
	exp = parts.pop ();
	ifStatement = parts.pop ();
	elseStatement = r.size () > 5 ? parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + " " + ifStatement + " " + elseStatement + "}";
    }
}
