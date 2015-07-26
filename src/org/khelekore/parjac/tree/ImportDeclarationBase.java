package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public abstract class ImportDeclarationBase extends NamedNode implements ImportDeclaration {
    private boolean used = false;

    public ImportDeclarationBase (DottedName name, ParsePosition pos) {
	super (name, pos);
    }

    public ImportDeclarationBase (Deque<TreeNode> parts, ParsePosition pos) {
	super (parts, pos);
    }

    @Override public void markUsed () {
	used = true;
    }

    @Override public boolean hasBeenUsed () {
	return used;
    }
}