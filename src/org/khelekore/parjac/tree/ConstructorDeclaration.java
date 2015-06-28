package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ConstructorDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final ConstructorDeclarator declarator;
    private final Throws throwsClause;
    private final ConstructorBody body;

    public ConstructorDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
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

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endConstructor (this);
    }

    public TypeParameters getTypeParameters () {
	return declarator.getTypeParameters ();
    }

    public List<TreeNode> getModifiers () {
	return modifiers;
    }

    public FormalParameterList getParameters () {
	return declarator.getParameters ();
    }
}
