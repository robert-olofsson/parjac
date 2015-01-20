package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class DottedName implements TreeNode {
    private final List<String> parts;

    public DottedName (List<String> parts) {
	this.parts = parts;
    }

    public DottedName (String... parts) {
	this.parts = new ArrayList<> (Arrays.asList (parts));
    }

    public static TreeNode create (Rule r, Deque<TreeNode> parts) {
	DottedName dn;
	if (r.size () > 1)
	    dn = (DottedName)parts.pop ();
	else
	    dn = new DottedName (new ArrayList<> ());

	dn.parts.add (((Identifier)parts.pop ()).get ());
	return dn;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + parts + "}";
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	DottedName dn = (DottedName)o;
	return parts.equals (dn.parts);
    }
}