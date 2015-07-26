package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ExtraName extends PositionNode {
    private final List<TreeNode> annotations;
    private final String id;

    public ExtraName (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	annotations = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	id = ((Identifier)parts.pop ()).get ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{." + annotations + " " + id + "}";
    }

    public SimpleClassType toSimpleClassType () {
	return new SimpleClassType (annotations, id, null, getParsePosition ());
    }
}
