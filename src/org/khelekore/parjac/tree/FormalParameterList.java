package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;
import java.util.function.Function;

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
	appendDescription (sb, fp -> fp.getExpressionType ().getDescriptor ());
    }

    public void appendGenericDescription (StringBuilder sb) {
	appendDescription (sb, fp -> GenericTypeHelper.getGenericType (fp.getType ()));
    }

    private void appendDescription (StringBuilder sb, Function<FormalParameter, String> f) {
	NormalFormalParameterList nfpl = getParameters ();
	if (nfpl != null) {
	    List<FormalParameter> ls = nfpl.getFormalParameters ();
	    if (ls != null)
		ls.stream ().forEach (fp -> sb.append (f.apply (fp)));
	    LastFormalParameter lfp = nfpl.getLastFormalParameter ();
	    if (lfp != null)
		sb.append (lfp.getExpressionType ().getDescriptor ());
	}
    }
}
