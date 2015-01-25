package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class UnannClassType extends ClassType {
    public UnannClassType (SimpleClassType sct) {
	super (sct);
    }

    public static ClassType build (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1 || r.size () == 2) {
	    SimpleClassType sct = new SimpleClassType (r, parts);
	    return new UnannClassType (sct);
	}
	UnannClassType ct = (UnannClassType)parts.pop ();
	ct.add ((SimpleClassType)parts.pop ());
	return ct;
    }
}
