package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class NormalClassDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final String id;
    private final TypeParameters types;
    private final ClassType superclass;
    private final InterfaceTypeList superinterfaces;
    private final ClassBody body;

    public NormalClassDeclaration (Rule r, Deque<TreeNode> parts) {
	int pos = 2;
	TreeNode tn = parts.pop ();
	if (r.getRulePart (1).getId () != Token.IDENTIFIER) {
	    modifiers = ((ZOMEntry)tn).get ();
	    tn = parts.pop ();
	    pos++;
	} else {
	    modifiers = null;
	}
	id = ((Identifier)tn).get ();
	if (r.getRulePart (pos).getId ().equals ("TypeParameters")) {
	    types = (TypeParameters)parts.pop ();
	    pos++;
	} else {
	    types = null;
	}
	if (r.getRulePart (pos).getId ().equals ("Superclass")) {
	    superclass = (ClassType)parts.pop ();
	    pos++;
	} else {
	    superclass = null;
	}
	if (r.getRulePart (pos).getId ().equals ("Superinterfaces")) {
	    superinterfaces = (InterfaceTypeList)parts.pop ();
	} else {
	    superinterfaces = null;
	}
	body = (ClassBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + id +
	    " " + types + " " + superclass + " " + superinterfaces + " " + body + "}";
    }
}
