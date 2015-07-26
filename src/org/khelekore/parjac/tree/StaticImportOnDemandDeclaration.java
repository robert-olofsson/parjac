package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class StaticImportOnDemandDeclaration extends ImportDeclarationBase {
    public StaticImportOnDemandDeclaration (DottedName name, ParsePosition ppos) {
	super (name, ppos);
    }

    public void visit (ImportVisitor iv) {
	iv.visit (this);
    }

    public static StaticImportOnDemandDeclaration build (Deque<TreeNode> parts, ParsePosition pos) {
	parts.pop (); // static
	DottedName name = (DottedName)parts.pop ();
	parts.pop (); // '*'
	return new StaticImportOnDemandDeclaration (name, pos);
    }
}
