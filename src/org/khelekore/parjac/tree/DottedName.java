package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class DottedName implements TreeNode {
    private final List<String> parts = new ArrayList<> ();

    private DottedName () {
    }

    private void add (String identifier) {
	parts.add (identifier);
    }

    public static TreeNode create (Rule r, Deque<TreeNode> parts) {
	DottedName dn;
	if (r.size () > 1)
	    dn = (DottedName)parts.pop ();
	else
	    dn = new DottedName ();

	dn.add (((Identifier)parts.pop ()).get ());
	return dn;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + parts + "}";
    }
}