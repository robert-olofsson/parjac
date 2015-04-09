package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class CatchClause implements TreeNode {
    private final CatchFormalParameter cfp;
    private final Block block;

    public CatchClause (Deque<TreeNode> parts, ParsePosition ppos) {
	cfp = (CatchFormalParameter)parts.pop ();
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + cfp + " " + block + "}";
    }
}
