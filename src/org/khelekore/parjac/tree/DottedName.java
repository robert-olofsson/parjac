package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class DottedName extends PositionNode {
    private final List<String> parts;

    public DottedName (List<String> parts, ParsePosition pos) {
	super (pos);
	this.parts = parts;
    }

    public static TreeNode create (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	DottedName dn;
	if (r.size () > 1)
	    dn = (DottedName)parts.pop ();
	else
	    dn = new DottedName (new ArrayList<> (), pos);

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

    public int size () {
	return parts.size ();
    }

    public List<String> getParts () {
	return parts;
    }

    public String getPathName () {
	return parts.stream ().collect (Collectors.joining ("/"));
    }

    public String getDotName () {
	return parts.stream ().collect (Collectors.joining ("."));
    }

    public String getLastPart () {
	return parts.get (parts.size () - 1);
    }

    public ClassType toClassType () {
	String name = parts.get (0);
	ParsePosition pos = getParsePosition ();
	ClassType ct = new ClassType (new SimpleClassType (null, name, null, pos), pos);
	for (int i = 1; i < parts.size (); i++) {
	    ct.add (new SimpleClassType (null, name, null, pos));
	}
	return ct;
    }
}