package org.khelekore.parjac.tree;

import java.util.Deque;

public class SingleTypeImportDeclaration extends NamedNode implements ImportDeclaration {
    public SingleTypeImportDeclaration (Deque<TreeNode> parts) {
	super (parts);
    }
}
