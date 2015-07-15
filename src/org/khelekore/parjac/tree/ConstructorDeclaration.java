package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ConstructorDeclaration extends PositionNode {
    private final List<TreeNode> annotations;
    private final int flags;
    private final ConstructorDeclarator declarator;
    private final Throws throwsClause;
    private final ConstructorBody body;

    public ConstructorDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
				   Path path, CompilerDiagnosticCollector diagnostics) {
	super (ppos);
	int pos = 1;
	List<TreeNode> modifiers;
	if (!r.getRulePart (0).getId ().equals ("ConstructorDeclarator")) {
	    modifiers = ((ZOMEntry)parts.pop ()).get ();
	    pos++;
	} else {
	    modifiers = null;
	}
	annotations = ModifierHelper.getAnnotations (modifiers);
	flags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
	declarator = (ConstructorDeclarator)parts.pop ();
	if (r.getRulePart (pos).getId ().equals ("Throws")) {
	    throwsClause = (Throws)parts.pop ();
	} else {
	    throwsClause = null;
	}
	body = (ConstructorBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + " " + flags + " " +
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

    public List<TreeNode> getAnnotations () {
	return annotations;
    }

    public int getFlags () {
	return flags;
    }

    public FormalParameterList getParameters () {
	return declarator.getParameters ();
    }
}
