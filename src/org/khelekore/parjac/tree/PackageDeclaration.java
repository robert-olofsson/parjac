package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class PackageDeclaration implements TreeNode {
    private final List<TreeNode> annotations;
    private final DottedName name;

    public PackageDeclaration (Rule r, Deque<TreeNode> parts) {
	annotations = r.size () > 1 ? ((ZOMEntry)parts.pop ()).get () : null;
	name = (DottedName)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations +
	    ", name: " + name + "}";
    }
}