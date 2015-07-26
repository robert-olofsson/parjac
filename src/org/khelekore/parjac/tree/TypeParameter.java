package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class TypeParameter extends PositionNode {
    private final List<TreeNode> annotations;
    private final String id;
    private final TypeBound typeBound;

    public TypeParameter (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	int len = 1;
	if (r.getRulePart (0).getId () != Token.IDENTIFIER) {
	    annotations = ((ZOMEntry)parts.pop ()).get ();
	    len++;
	} else {
	    annotations = null;
	}
	id = ((Identifier)parts.pop ()).get ();
	typeBound = r.size () > len ? (TypeBound)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + " " + id + " " + typeBound + "}";
    }

    public String getId () {
	return id;
    }
}
