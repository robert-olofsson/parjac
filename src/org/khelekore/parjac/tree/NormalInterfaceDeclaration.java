package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class NormalInterfaceDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final String id;
    private final TypeParameters types;
    private final ExtendsInterfaces extendsInterfaces;
    private final InterfaceBody body;

    public NormalInterfaceDeclaration (Rule r, Deque<TreeNode> parts) {
	int pos = 2;
	if (r.getRulePart (0).getId () != Token.INTERFACE) {
	    modifiers = ((ZOMEntry)parts.pop ()).get ();
	    pos++;
	} else {
	    modifiers = null;
	}
	id = ((Identifier)parts.pop ()).get ();
	if (r.getRulePart (pos).getId ().equals ("TypeParameters")) {
	    types = (TypeParameters)parts.pop ();
	    pos++;
	} else {
	    types = null;
	}
	if (r.getRulePart (pos).getId ().equals ("ExtendsInterfaces")) {
	    extendsInterfaces = (ExtendsInterfaces)parts.pop ();
	    pos++;
	} else {
	    extendsInterfaces = null;
	}
	body = (InterfaceBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + id + " " +
	    types + " " + extendsInterfaces + " " + body + "}";
    }
}
