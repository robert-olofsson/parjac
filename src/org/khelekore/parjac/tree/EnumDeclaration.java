package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class EnumDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final String id;
    private final InterfaceTypeList superInterfaces;
    private final EnumBody body;

    public EnumDeclaration (Rule r, Deque<TreeNode> parts) {
	int pos = 2;
	if (r.getRulePart (0).getId () != Token.ENUM) {
	    pos++;
	    modifiers = ((ZOMEntry)parts.pop ()).get ();
	} else {
	    modifiers = null;
	}
	id = ((Identifier)parts.pop ()).get ();
	superInterfaces = r.size () > pos + 1 ? (InterfaceTypeList)parts.pop () : null;
	body = (EnumBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers +
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
	visitor.visit (this);
	body.visit (visitor);
	visitor.endType ();
    }
}
