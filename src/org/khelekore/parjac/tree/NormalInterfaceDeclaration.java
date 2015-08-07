package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class NormalInterfaceDeclaration extends FlaggedType {
    private final String id;
    private final TypeParameters types;
    private final ExtendsInterfaces extendsInterfaces;
    private final InterfaceBody body;

    public NormalInterfaceDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
				       Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.getRulePart (0).getId () != Token.INTERFACE, parts, ppos, path, diagnostics);
	int pos = r.getRulePart (0).getId () != Token.INTERFACE ? 3 : 2;
	id = ((Identifier)parts.pop ()).get ();
	if (r.getRulePart (pos).getId ().equals ("TypeParameters")) {
	    types = (TypeParameters)parts.pop ();
	    pos++;
	} else {
	    types = null;
	}
	if (r.getRulePart (pos).getId ().equals ("ExtendsInterfaces")) {
	    extendsInterfaces = (ExtendsInterfaces)parts.pop ();
	    pos++;
	} else {
	    extendsInterfaces = null;
	}
	body = (InterfaceBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getAnnotations () + ", " + getFlags () + " " +
	    id + ", " + types + ", " + extendsInterfaces + ", " + body + "}";
    }

    public String getId () {
	return id;
    }

    public TypeParameters getTypeParameters () {
	return types;
    }

    public ExtendsInterfaces getExtendsInterfaces () {
	return extendsInterfaces;
    }

    public InterfaceBody getBody () {
	return body;
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endType ();
    }
}
