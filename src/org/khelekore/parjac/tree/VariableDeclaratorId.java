package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class VariableDeclaratorId extends PositionNode {
    private final String id;
    private final Dims dims;

    public VariableDeclaratorId (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	id = ((Identifier)parts.pop ()).get ();
	dims = r.size () > 1 ? (Dims)parts.pop () : null;
    }

    public VariableDeclaratorId (String id) {
	super (null);
	this.id = id;
	dims = null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{id: " + id + ", dims: " + dims + "}";
    }

    public String getId () {
	return id;
    }
}