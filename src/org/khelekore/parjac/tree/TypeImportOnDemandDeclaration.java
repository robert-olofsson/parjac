package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class TypeImportOnDemandDeclaration extends NamedNode implements ImportDeclaration {
    public TypeImportOnDemandDeclaration (Deque<TreeNode> parts, ParsePosition pos) {
	super (parts, pos);
	parts.pop (); // '*'
    }

    public void visit (ImportVisitor iv) {
	iv.visit (this);
    }
}
