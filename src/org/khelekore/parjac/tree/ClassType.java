package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ClassType extends Multipart<SimpleClassType> {
    private String fqn;
    private TypeParameter tp;

    public ClassType (SimpleClassType sct, ParsePosition pos) {
	super (sct, pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + get () + ", fqn: " + fqn + ", tp: " + tp + "}";
    }

    public void setFullName (String fqn) {
	this.fqn = fqn;
    }

    public String getFullName () {
	return fqn;
    }

    public String getSlashName () {
	if (fqn == null) System.err.println ("ugh: " + this);
	return fqn.replace ('.', '/');
    }

    public void setTypeParameter (TypeParameter tp) {
	this.tp = tp;
    }

    public TypeParameter getTypeParameter () {
	return tp;
    }

    public TypeArguments getTypeArguments () {
	List<SimpleClassType> ls = get ();
	return ls.get (ls.size () - 1).getTypeArguments ();
    }

    @Override public ExpressionType getExpressionType () {
	if (fqn != null)
	    return ExpressionType.getObjectType (fqn);
	return null;
    }

    public static ClassType build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 1)
	    return new ClassType ((SimpleClassType)parts.pop (), pos);
	ClassType ct = (ClassType)parts.pop ();
	ct.add ((SimpleClassType)parts.pop ());
	return ct;
    }
}
