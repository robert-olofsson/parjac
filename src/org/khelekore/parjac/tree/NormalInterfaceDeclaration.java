package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class NormalInterfaceDeclaration extends PositionNode {
    private final List<Annotation> annotations;
    private final int accessFlags;
    private final String id;
    private final TypeParameters types;
    private final ExtendsInterfaces extendsInterfaces;
    private final InterfaceBody body;

    public NormalInterfaceDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
	int pos = 2;
	List<TreeNode> modifiers;
	if (r.getRulePart (0).getId () != Token.INTERFACE) {
	    modifiers = ((ZOMEntry)parts.pop ()).get ();
	    pos++;
	} else {
	    modifiers = null;
	}
	annotations = ModifierHelper.getAnnotations (modifiers);
	accessFlags = ModifierHelper.getModifiers (modifiers);

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
	return getClass ().getSimpleName () + "{" + annotations + ", " + accessFlags + " " +
	    id + ", " + types + ", " + extendsInterfaces + ", " + body + "}";
    }

    public int getAccessFlags () {
	return accessFlags;
    }

    public List<Annotation> getAnntations () {
	return annotations;
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
