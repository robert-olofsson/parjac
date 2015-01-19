package org.khelekore.parjac.tree;

import java.util.Deque;

public class SingleStaticImportDeclaration extends NamedNode implements ImportDeclaration {
    private final Identifier id;

    public SingleStaticImportDeclaration (Deque<TreeNode> parts) {
	super (parts);
	this.id = (Identifier)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + getName () + ", identifier: " + id + "}";
    }
}
