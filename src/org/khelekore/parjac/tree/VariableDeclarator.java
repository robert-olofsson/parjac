package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
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

    public VariableDeclarator (VariableDeclaratorId vdi, TreeNode initializer, ParsePosition pos) {
	super (pos);
	this.vdi = vdi;
	this.initializer = initializer;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{vdi: " + vdi + " = " + initializer + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    if (initializer != null)
		initializer.visit (visitor);
	}
    }

    public Collection<? extends TreeNode> getChildNodes () {
	if (initializer != null)
	    return Collections.singleton (initializer);
	return Collections.emptyList ();
    }

    public String getId () {
	return vdi.getId ();
    }

    public TreeNode getInitializer () {
	return initializer;
    }

    public boolean hasInitializer () {
	return initializer != null;
    }
}