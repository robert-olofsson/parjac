package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class NormalClassDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final String id;
    private final ClassBody body;

    public NormalClassDeclaration (Rule r, Deque<TreeNode> parts) {
	TreeNode tn = parts.pop ();
	if (r.getRulePart (0).getId () != Token.IDENTIFIER) {
	    modifiers = ((ZOMEntry)tn).get ();
	    tn = parts.pop ();
	} else {
	    modifiers = null;
	}
	id = ((Identifier)tn).get ();
	// TODO: handle the rest of the stuff
	body = (ClassBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{modifiers: " + modifiers +
	    ", id: " + id + ", body: " + body + "}";
    }
}
