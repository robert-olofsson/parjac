package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class MethodDeclaration extends FlaggedTypeBase {
    private final MethodHeader header;
    private final MethodBody body;

    public MethodDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos,
			      Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.size () > 2, parts, pos, path, diagnostics);
	header = (MethodHeader)parts.pop ();
	body = (MethodBody)parts.pop ();
	if (isVarArgs ())
	    setVarArgs ();
    }

    public MethodDeclaration (List<TreeNode> modifiers, MethodHeader header, MethodBody body) {
	super (modifiers, null, null, null);
	this.header = header;
	this.body = body;
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

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (body);
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

    public Throws getThrows () {
	return header.getThrows ();
    }

    public MethodBody getBody () {
	return body;
    }

    @Override public ExpressionType getExpressionType () {
	return getResult ().getExpressionType ();
    }

    private boolean isVarArgs () {
	FormalParameterList fpl = getParameters ();
	return fpl != null && fpl.isVarArgs ();
    }

    public String getDescription () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("(");
	FormalParameterList fpl = getParameters ();
	if (fpl != null)
	    fpl.appendDescription (sb);
	sb.append (")");
	sb.append (getResult ().getExpressionType ().getDescriptor ());
	return sb.toString ();
    }
}
