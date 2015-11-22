package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class NormalClassDeclaration extends FlaggedType {
    private final String id;
    private final TypeParameters types;
    private final ClassType superclass;
    private final InterfaceTypeList superinterfaces;
    private final ClassBody body;

    public NormalClassDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
				   Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.getRulePart (1).getId () != Token.IDENTIFIER, parts, ppos, path, diagnostics);
	int pos = r.getRulePart (1).getId () != Token.IDENTIFIER ? 3 : 2;
	TreeNode tn = parts.pop ();
	id = ((Identifier)tn).get ();
	if (r.getRulePart (pos).getId ().equals ("TypeParameters")) {
	    types = (TypeParameters)parts.pop ();
	    pos++;
	} else {
	    types = null;
	}
	if (r.getRulePart (pos).getId ().equals ("Superclass")) {
	    superclass = (ClassType)parts.pop ();
	    pos++;
	} else {
	    superclass = null;
	}
	if (r.getRulePart (pos).getId ().equals ("Superinterfaces")) {
	    superinterfaces = (InterfaceTypeList)parts.pop ();
	} else {
	    superinterfaces = null;
	}
	body = (ClassBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getAnnotations () + ", " + getFlags () + " " +
	    id + " " + types + " " + superclass + " " + superinterfaces + " " + body + "}";
    }

    public String getId () {
	return id;
    }

    public TypeParameters getTypeParameters () {
	return types;
    }

    public ClassType getSuperClass () {
	return superclass;
    }

    public InterfaceTypeList getSuperInterfaces () {
	return superinterfaces;
    }

    public ClassBody getBody () {
	return body;
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endType ();
    }

    @Override public ExpressionType getExpressionType () {
	return null;
    }
}
