package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class EnumDeclaration extends PositionNode {
    private final List<Annotation> annotations;
    private final int accessFlags;
    private final String id;
    private final InterfaceTypeList superInterfaces;
    private final EnumBody body;

    public EnumDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
	int pos = 2;
	List<TreeNode> modifiers;
	if (r.getRulePart (0).getId () != Token.ENUM) {
	    pos++;
	    modifiers = ((ZOMEntry)parts.pop ()).get ();
	} else {
	    modifiers = null;
	}
	annotations = ModifierHelper.getAnnotations (modifiers);
	accessFlags = ModifierHelper.getModifiers (modifiers);
	id = ((Identifier)parts.pop ()).get ();
	superInterfaces = r.size () > pos + 1 ? (InterfaceTypeList)parts.pop () : null;
	body = (EnumBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + ", " + accessFlags +
	    ", id: " +  id + ", superInterfaces: " + superInterfaces + ", body: " + body + "}";
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
