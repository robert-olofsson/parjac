package org.khelekore.parjac.tree;

import java.util.Deque;

public class TypeImportOnDemandDeclaration extends NamedNode implements ImportDeclaration {
    public TypeImportOnDemandDeclaration (Deque<TreeNode> parts) {
	super (parts);
	parts.pop (); // '*'
    }

    public void visit (ImportVisitor iv) {
	iv.visit (this);
    }
}
