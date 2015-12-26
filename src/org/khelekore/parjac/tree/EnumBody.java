package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.lexer.ParsePosition;

public class EnumBody extends PositionNode {
    private final EnumConstantList constants;
    private final EnumBodyDeclarations enumBodyDeclarations;

    public EnumBody (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
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

    @Override public void visit (TreeVisitor visitor) {
	if (constants != null)
	    constants.visit (visitor);
	if (enumBodyDeclarations != null)
	    enumBodyDeclarations.visit (visitor);
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	if (enumBodyDeclarations != null)
	    return Collections.singleton (enumBodyDeclarations);
	return Collections.emptyList ();
    }
}
