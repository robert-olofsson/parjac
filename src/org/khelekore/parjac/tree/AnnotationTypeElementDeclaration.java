package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class AnnotationTypeElementDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final TreeNode type;
    private final String id;
    private final Dims dims;
    private final DefaultValue defaultValue;

    public AnnotationTypeElementDeclaration (Rule r, Deque<TreeNode> parts) {
	int pos = 4;
	if (r.getRulePart (1).getId () != Token.IDENTIFIER) {
	    modifiers = ((ZOMEntry)parts.pop ()).get ();
	    pos++;
	} else {
	    modifiers = null;
	}
	type = parts.pop ();
	id = ((Identifier)parts.pop ()).get ();
	if (r.getRulePart (pos).getId ().equals ("Dims")) {
	    dims = (Dims)parts.pop ();
	    pos++;
	} else {
	    dims = null;
	}
	if (r.getRulePart (pos).getId ().equals ("DefaultValue")) {
	    defaultValue = (DefaultValue)parts.pop ();
	} else {
	    defaultValue = null;
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " +
	    type + " " + id + "()" + dims + " " + defaultValue + "}";
    }
}
