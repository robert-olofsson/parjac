package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.lexer.Token;

public class EnumDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final String id;
    private final EnumBody body;

    public EnumDeclaration (Rule r, Deque<TreeNode> parts) {
	int pos = 0;
	if (r.getRulePart (pos).getId () != Token.ENUM) {
	    pos++;
	    modifiers = ((ZOMEntry)parts.pop ()).get ();
	} else {
	    modifiers = null;
	}
	id = ((Identifier)parts.pop ()).get ();
	pos += 2;
	if (r.size () > pos + 2)
	    parts.pop (); // TODO: superinterfaces
	body = (EnumBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers +
	    ", id: " +  id + ", body: " + body + "}";
    }
}
