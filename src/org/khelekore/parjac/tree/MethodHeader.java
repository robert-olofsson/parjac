package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class MethodHeader extends PositionNode {
    private final TypeParameters typeParameters;
    private final List<TreeNode> annotations;
    private final UntypedMethodHeader untypedHeader;

    public MethodHeader (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	typeParameters = r.size () > 1 ? (TypeParameters)parts.pop () : null;
	annotations = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	untypedHeader = (UntypedMethodHeader)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + typeParameters + " " +
	    annotations + " " + untypedHeader + "}";
    }

    public TypeParameters getTypeParameters () {
	return typeParameters;
    }

    public Result getResult () {
	return untypedHeader.getResult ();
    }

    public String getMethodName () {
	return untypedHeader.getMethodName ();
    }

    public FormalParameterList getParameters () {
	return untypedHeader.getParameters ();
    }

    public Throws getThrows () {
	return untypedHeader.getThrows ();
    }
}
