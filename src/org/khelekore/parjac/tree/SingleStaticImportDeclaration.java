package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class SingleStaticImportDeclaration extends ImportDeclarationBase {
    private final String id;

    public SingleStaticImportDeclaration (DottedName name, String id, ParsePosition ppos) {
	super (name, ppos);
	this.id = id;
    }

    public void visit (ImportVisitor iv) {
	iv.visit (this);
    }

    public static SingleStaticImportDeclaration build (Deque<TreeNode> parts, ParsePosition pos) {
	parts.pop (); // 'static'
	return new SingleStaticImportDeclaration ((DottedName)parts.pop (),
						  ((Identifier)parts.pop ()).get (),
						  pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + getName () + ", identifier: " + id + "}";
    }

    public String getInnerId () {
	return id;
    }

    public String getFullName () {
	return getName ().getDotName () + "$" + id;
    }
}
