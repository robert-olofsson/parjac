package org.khelekore.parjac.tree;

import java.util.Deque;

public class StaticImportOnDemandDeclaration extends NamedNode implements ImportDeclaration {
    public StaticImportOnDemandDeclaration (Deque<TreeNode> parts) {
	super (parts);
    }
}
