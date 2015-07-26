package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class NormalFormalParameterList extends PositionNode {
    private final List<FormalParameter> fps;
    private final LastFormalParameter lfp;

    public NormalFormalParameterList (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
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

    public List<FormalParameter> getFormalParameters () {
	return fps;
    }

    public LastFormalParameter getLastFormalParameter () {
	return lfp;
    }
}
