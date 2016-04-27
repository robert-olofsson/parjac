package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class MethodDeclarator extends PositionNode {
    private final String id;
    private final FormalParameterList parameterList;
    private final Dims dims;

    public MethodDeclarator (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	id = ((Identifier)parts.pop ()).get ();
	int len = 3;
	if (r.getRulePart (2).getId () != Token.RIGHT_PARENTHESIS) {
	    parameterList = (FormalParameterList)parts.pop ();
	    len++;
	} else {
	    parameterList = null;
	}
	dims = r.size () > len ? (Dims)parts.pop () : null;
    }

    public MethodDeclarator (String id, FormalParameterList parameterList, Dims dims) {
	super (null);
	this.id = id;
	this.parameterList = parameterList;
	this.dims = dims;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + "(" + parameterList + ")" + dims + "}";
    }

    public String getId () {
	return id;
    }

    public FormalParameterList getParameters () {
	return parameterList;
    }
}
