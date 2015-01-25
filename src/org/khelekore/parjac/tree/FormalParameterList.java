package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class FormalParameterList implements TreeNode {
    private final ReceiverParameter rp;
    private final NormalFormalParameterList fps;

    public FormalParameterList (Rule r, Deque<TreeNode> parts) {
	rp = r.size () > 1 ? (ReceiverParameter)parts.pop () : null;
	fps = (NormalFormalParameterList)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + rp + " " + fps + "}";
    }
}
