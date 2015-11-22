package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ConstructorDeclaration extends FlaggedType {
    private final ConstructorDeclarator declarator;
    private final Throws throwsClause;
    private final ConstructorBody body;

    public ConstructorDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
				   Path path, CompilerDiagnosticCollector diagnostics) {
	super (!r.getRulePart (0).getId ().equals ("ConstructorDeclarator"), parts, ppos, path, diagnostics);
	declarator = (ConstructorDeclarator)parts.pop ();
	if (parts.peek () instanceof Throws) {
	    throwsClause = (Throws)parts.pop ();
	} else {
	    throwsClause = null;
	}
	body = (ConstructorBody)parts.pop ();
    }

    public ConstructorDeclaration (ConstructorDeclarator declarator, Throws throwsClause, ConstructorBody body) {
	super (Collections.emptyList (), null, null, null);
	this.declarator = declarator;
	this.throwsClause = throwsClause;
	this.body = body;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getAnnotations () + " " + getFlags () +
	    " " + declarator + " " + throwsClause + " " + body + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endConstructor (this);
    }

    public String getId () {
	return declarator.getId ();
    }

    public TypeParameters getTypeParameters () {
	return declarator.getTypeParameters ();
    }

    public FormalParameterList getParameters () {
	return declarator.getParameters ();
    }

    public String getDescription () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("(");
	FormalParameterList fpl = getParameters ();
	if (fpl != null)
	    fpl.appendDescription (sb);
	sb.append (")V");
	return sb.toString ();
    }
}
