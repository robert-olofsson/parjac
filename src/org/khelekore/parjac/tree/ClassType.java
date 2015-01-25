package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ClassType extends Multipart<SimpleClassType> {
    public ClassType (SimpleClassType sct) {
	super (sct);
    }

    public static ClassType build (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    return new ClassType ((SimpleClassType)parts.pop ());
	ClassType ct = (ClassType)parts.pop ();
	ct.add ((SimpleClassType)parts.pop ());
	return ct;
    }
}
