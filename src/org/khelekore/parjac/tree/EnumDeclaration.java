package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class EnumDeclaration extends FlaggedType {
    private final String id;
    private final InterfaceTypeList superInterfaces;
    private final EnumBody body;

    public EnumDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
			    Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.getRulePart (0).getId () != Token.ENUM, parts, ppos, path, diagnostics);
	int pos = r.getRulePart (0).getId () != Token.ENUM ? 3 : 2;
	id = ((Identifier)parts.pop ()).get ();
	superInterfaces = r.size () > pos + 1 ? (InterfaceTypeList)parts.pop () : null;
	body = (EnumBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getAnnotations () + ", " + getFlags () +
	    ", id: " +  id + ", superInterfaces: " + superInterfaces + ", body: " + body + "}";
    }

    public String getId () {
	return id;
    }

    public InterfaceTypeList getSuperInterfaces () {
	return superInterfaces;
    }

    public EnumBody getBody () {
	return body;
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endType ();
    }
}
