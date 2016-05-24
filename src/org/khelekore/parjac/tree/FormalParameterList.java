package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class FormalParameterList extends PositionNode {
    private final ReceiverParameter rp;
    private final NormalFormalParameterList fps;

    public FormalParameterList (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	if (r.getRulePart (0).getId ().equals ("ReceiverParameter"))
	    rp = (ReceiverParameter)parts.pop ();
	else
	    rp = null;
	fps = rp == null || r.size () > 2 ? (NormalFormalParameterList)parts.pop () : null;
    }

    public FormalParameterList (NormalFormalParameterList fps) {
	super (null);
	this.rp = null;
	this.fps = fps;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + rp + " " + fps + "}";
    }

    public NormalFormalParameterList getParameters () {
	return fps;
    }

    public boolean isVarArgs () {
	return fps != null && fps.isVarArgs ();
    }

    public void appendDescription (StringBuilder sb) {
	NormalFormalParameterList nfpl = getParameters ();
	if (nfpl != null) {
	    List<FormalParameter> ls = nfpl.getFormalParameters ();
	    if (ls != null) {
		for (FormalParameter fp : ls) {
		    ExpressionType et = fp.getExpressionType ();
		    sb.append (et.getDescriptor ());
		}
	    }
	    LastFormalParameter lfp = nfpl.getLastFormalParameter ();
	    if (lfp != null)
		sb.append (lfp.getExpressionType ().getDescriptor ());
	}
    }
}
