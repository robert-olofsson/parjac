package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.khelekore.parjac.grammar.Rule;

public class PackageDeclaration implements TreeNode {
    private final List<TreeNode> annotations;
    private final DottedName name;

    public PackageDeclaration (List<TreeNode> annotations, DottedName name) {
	this.annotations = annotations;
	this.name = name;
    }

    public PackageDeclaration (Rule r, Deque<TreeNode> parts) {
	annotations = r.size () > 3 ? ((ZOMEntry)parts.pop ()).get () : null;
	name = (DottedName)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations +
	    ", name: " + name + "}";
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	PackageDeclaration pd = (PackageDeclaration)o;
	return Objects.equals (annotations, pd.annotations) && name.equals (pd.name);
    }
}