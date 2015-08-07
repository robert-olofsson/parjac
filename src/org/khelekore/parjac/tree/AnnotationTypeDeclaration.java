package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class AnnotationTypeDeclaration extends FlaggedType {
    private final String id;
    private final AnnotationTypeBody body;

    public AnnotationTypeDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
				      Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.size () > 4, parts, ppos, path, diagnostics);
	id = ((Identifier)parts.pop ()).get ();
	body = (AnnotationTypeBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () +
	    "{" + getAnnotations () + ", " + getFlags () + " " + id + " " + body + "}";
    }

    public String getId () {
	return id;
    }

    public AnnotationTypeBody getBody () {
	return body;
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endType ();
    }
}
