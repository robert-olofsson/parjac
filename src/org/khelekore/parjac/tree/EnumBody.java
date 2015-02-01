package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.SimplePart;

public class EnumBody implements TreeNode {
    private final EnumConstantList constants;
    private final EnumBodyDeclarations enumBodyDeclarations;

    public EnumBody (Rule r, Deque<TreeNode> parts) {
	EnumConstantList constants = null;
	EnumBodyDeclarations enumBodyDeclarations = null;
	for (SimplePart sp : r.getParts ()) {
	    if (sp.getId ().equals ("EnumConstantList"))
		constants = (EnumConstantList)parts.pop ();
	    else if (sp.getId ().equals ("EnumBodyDeclarations"))
		enumBodyDeclarations = (EnumBodyDeclarations)parts.pop ();
	}
	this.constants = constants;
	this.enumBodyDeclarations = enumBodyDeclarations;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + constants +
	    ", enumBodyDeclarations: " + enumBodyDeclarations + "}";
    }

    public List<TreeNode> getDeclarations () {
	return enumBodyDeclarations.getDeclarations ();
    }

    public void visit (TreeVisitor visitor) {
	enumBodyDeclarations.visit (visitor);
    }
}
