package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class ConstructorDeclarator implements TreeNode {
    private final TypeParameters types;
    private final String id;
    private final FormalParameterList params;

    public ConstructorDeclarator (Rule r, Deque<TreeNode> parts) {
	int pos = 2;
	if (r.getRulePart (0).getId () != Token.IDENTIFIER) {
	    types = (TypeParameters)parts.pop ();
	    pos++;
	} else {
	    types = null;
	}
	id = ((Identifier)parts.pop ()).get ();
	if (r.getRulePart (pos).getId () != Token.RIGHT_PARENTHESIS) {
	    params = (FormalParameterList)parts.pop ();
	} else {
	    params = null;
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + types + " " + id + "(" + params + ")}";
    }
}