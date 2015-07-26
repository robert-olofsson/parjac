package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class VariableDeclarator extends PositionNode {
    private final VariableDeclaratorId vdi;
    private final TreeNode initializer;

    public VariableDeclarator (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	vdi = (VariableDeclaratorId)parts.pop ();
	if (r.size () > 1) {
	    parts.pop (); // '='
	    initializer = parts.pop ();
	} else {
	    initializer = null;
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{vdi: " + vdi + " = " + initializer + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (initializer != null)
	    initializer.visit (visitor);
    }

    public String getId () {
	return vdi.getId ();
    }
}