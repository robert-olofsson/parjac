package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class TypeParameters extends PositionNode {
    private final List<TypeParameter> types;

    public TypeParameters (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	parts.pop (); // '<'
	TypeParameter tp = (TypeParameter)parts.pop ();
	if (r.size () == 3) {
	    types = Collections.singletonList (tp);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    types = ze.get ();
	    types.add (0, tp);
	}
	parts.pop (); // '>'
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + types + "}";
    }

    public List<TypeParameter> get () {
	return types;
    }
}
