package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ClassInstanceName extends PositionNode {
    private final String id;
    private final List<ExtraName> extras;

    public ClassInstanceName (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
	id = ((Identifier)parts.pop ()).get ();
	extras = r.size () > 1 ? ((ZOMEntry)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + " " + extras + "}";
    }

    public ClassType toClassType () {
	ClassType ct = new ClassType (new SimpleClassType (null, id, null), getParsePosition ());
	if (extras != null) {
	    for (ExtraName n : extras) {
		ct.add (n.toSimpleClassType ());
	    }
	}
	return ct;
    }
}
