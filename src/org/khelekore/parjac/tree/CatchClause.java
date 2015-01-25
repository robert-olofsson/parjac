package org.khelekore.parjac.tree;

import java.util.Deque;

public class CatchClause implements TreeNode {
    private final CatchFormalParameter cfp;
    private final Block block;

    public CatchClause (Deque<TreeNode> parts) {
	cfp = (CatchFormalParameter)parts.pop ();
	block = (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + cfp + " " + block + "}";
    }
}
