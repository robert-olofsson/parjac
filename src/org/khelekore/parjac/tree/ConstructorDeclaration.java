package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class ConstructorDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final ConstructorDeclarator declarator;
    private final Throws throwsClause;
    private final ConstructorBody body;

    public ConstructorDeclaration (Rule r, Deque<TreeNode> parts) {
	int pos = 1;
	if (!r.getRulePart (0).getId ().equals ("ConstructorDeclarator")) {
	    modifiers = ((ZOMEntry)parts.pop ()).get ();
	    pos++;
	} else {
	    modifiers = null;
	}
	declarator = (ConstructorDeclarator)parts.pop ();
	if (r.getRulePart (pos).getId ().equals ("Throws")) {
	    throwsClause = (Throws)parts.pop ();
	} else {
	    throwsClause = null;
	}
	body = (ConstructorBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " +
	    declarator + " " + throwsClause + " " + body + "}";
    }
}
