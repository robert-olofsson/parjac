package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class Throws extends PositionNode {
    private List<ClassType> exceptions;

    public Throws (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	ClassType ct = (ClassType)parts.pop ();
	if (r.size () == 2) {
	    exceptions = Collections.singletonList (ct);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    exceptions = new ArrayList<> (ze.size () + 1);
	    exceptions.add (ct);
	    exceptions.addAll (ze.get ());
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exceptions + "}";
    }

    public List<ClassType> getTypes () {
	return exceptions;
    }
}
