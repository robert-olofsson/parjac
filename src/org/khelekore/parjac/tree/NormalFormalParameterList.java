package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class NormalFormalParameterList implements TreeNode {
    private final List<FormalParameter> fps;
    private final LastFormalParameter lfp;

    public NormalFormalParameterList (Rule r, Deque<TreeNode> parts) {
	List<FormalParameter> fps =
	    r.size () > 1 ? ((ZOMEntry)parts.pop ()).get () : null;
	TreeNode tn = parts.pop ();
	if (tn instanceof FormalParameter) {
	    if (fps == null)
		fps = Collections.singletonList ((FormalParameter)tn);
	    else
		fps.add ((FormalParameter)tn);
	    lfp = null;
	} else {
	    lfp = (LastFormalParameter)tn;
	}
	this.fps = fps;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + fps + " " + lfp + "}";
    }
}
