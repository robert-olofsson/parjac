package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

/** A dotted name that we do not know what it is until after parse.
 *  It might be a fully qualified name "java.lang.String" or it might be
 *  a field access "a.b.c" or it might be a combination "a.b.C.d"
 */
public class DottedName extends PositionNode {
    private final List<String> parts;
    private TreeNode fieldAccess; // the actual nodes

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
	return getClass ().getSimpleName () + "{" + parts +
	    (fieldAccess != null ? ", fieldAccess: " + fieldAccess : "") + "}";
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

    public void setChainedFieldAccess (TreeNode fieldAccess) {
	this.fieldAccess = fieldAccess;
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	if (fieldAccess != null)
	    fieldAccess.visit (visitor);
    }

    @Override public ExpressionType getExpressionType () {
	if (fieldAccess != null)
	    return fieldAccess.getExpressionType ();
	return null;
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	if (fieldAccess != null)
	    return Collections.singleton (fieldAccess);
	return Collections.emptyList ();
    }

    public TreeNode getFieldAccess () {
	return fieldAccess;
    }
}