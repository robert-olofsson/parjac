package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class EnumConstantList implements TreeNode {
    private final List<EnumConstant> constants;

    public EnumConstantList (Rule r, Deque<TreeNode> parts) {
	EnumConstant ec = (EnumConstant)parts.pop ();
	if (r.size () == 1) {
	    constants = Collections.singletonList (ec);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    constants = new ArrayList<> (ze.size () + 1);
	    constants.add (ec);
	    constants.addAll (ze.get ());
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + constants + "}";
    }
}