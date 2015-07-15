package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class NormalClassDeclaration extends PositionNode {
    private final List<TreeNode> annotations;
    private final int accessFlags;
    private final String id;
    private final TypeParameters types;
    private final ClassType superclass;
    private final InterfaceTypeList superinterfaces;
    private final ClassBody body;

    public NormalClassDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
				   Path path, CompilerDiagnosticCollector diagnostics) {
	super (ppos);
	int pos = 2;
	TreeNode tn = parts.pop ();
	List<TreeNode> modifiers;
	if (r.getRulePart (1).getId () != Token.IDENTIFIER) {
	    modifiers = ((ZOMEntry)tn).get ();
	    tn = parts.pop ();
	    pos++;
	} else {
	    modifiers = Collections.emptyList ();
	}
	annotations = ModifierHelper.getAnnotations (modifiers);
	accessFlags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
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
	return getClass ().getSimpleName () + "{" + annotations + ", " + accessFlags + " " +
	    id + " " + types + " " + superclass + " " + superinterfaces + " " + body + "}";
    }

    public int getAccessFlags () {
	return accessFlags;
    }

    public List<TreeNode> getAnntations () {
	return annotations;
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
}
