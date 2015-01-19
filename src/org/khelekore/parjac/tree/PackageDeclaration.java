package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class PackageDeclaration implements TreeNode {
    private final ZOMEntry annotations;
    private final DottedName name;

    public PackageDeclaration (Rule r, Deque<TreeNode> parts) {
	annotations = r.size () > 1 ? (ZOMEntry)parts.pop () : null;
	name = (DottedName)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations +
	    ", name: " + name + "}";
    }
}