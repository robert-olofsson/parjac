package org.khelekore.parjac.tree;

import java.util.Deque;

public class StaticImportOnDemandDeclaration extends NamedNode implements ImportDeclaration {
    public StaticImportOnDemandDeclaration (DottedName name) {
	super (name);
    }

    public static StaticImportOnDemandDeclaration build (Deque<TreeNode> parts) {
	parts.pop (); // static
	DottedName name = (DottedName)parts.pop ();
	parts.pop (); // '*'
	return new StaticImportOnDemandDeclaration (name);
    }
}
