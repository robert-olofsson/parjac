package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class SingleTypeImportDeclaration extends ImportDeclarationBase {
    public SingleTypeImportDeclaration (Deque<TreeNode> parts, ParsePosition pos) {
	super (parts, pos);
    }

    public void visit (ImportVisitor iv) {
	iv.visit (this);
    }
}
