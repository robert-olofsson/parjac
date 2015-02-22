package org.khelekore.parjac.tree;

import java.util.Deque;

public class SingleStaticImportDeclaration extends NamedNode implements ImportDeclaration {
    private final String id;

    public SingleStaticImportDeclaration (DottedName name, String id) {
	super (name);
	this.id = id;
    }

    public void visit (ImportVisitor iv) {
	iv.visit (this);
    }

    public static SingleStaticImportDeclaration build (Deque<TreeNode> parts) {
	parts.pop (); // 'static'
	return new SingleStaticImportDeclaration ((DottedName)parts.pop (),
						  ((Identifier)parts.pop ()).get ());
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + getName () + ", identifier: " + id + "}";
    }
}
