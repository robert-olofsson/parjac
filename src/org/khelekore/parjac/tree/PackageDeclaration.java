package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class PackageDeclaration extends PositionNode {
    private final List<TreeNode> annotations;
    private final DottedName name;

    public PackageDeclaration (List<TreeNode> annotations, DottedName name, ParsePosition pos) {
	super (pos);
	this.annotations = annotations;
	this.name = name;
    }

    public PackageDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	annotations = r.size () > 3 ? ((ZOMEntry)parts.pop ()).get () : null;
	name = (DottedName)parts.pop ();
    }

    public DottedName getPackageName () {
	return name;
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