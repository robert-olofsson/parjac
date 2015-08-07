package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class MethodDeclaration extends FlaggedType {
    private final MethodHeader header;
    private final MethodBody body;

    public MethodDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos,
			      Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.size () > 2, parts, pos, path, diagnostics);
	header = (MethodHeader)parts.pop ();
	body = (MethodBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" +
	    getAnnotations () + " " + getFlags () + " " + " " + header + " " + body + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endMethod (this);
    }

    public TypeParameters getTypeParameters () {
	return header.getTypeParameters ();
    }

    public Result getResult () {
	return header.getResult ();
    }

    public String getMethodName () {
	return header.getMethodName ();
    }

    public FormalParameterList getParameters () {
	return header.getParameters ();
    }

    public MethodBody getBody () {
	return body;
    }
}
