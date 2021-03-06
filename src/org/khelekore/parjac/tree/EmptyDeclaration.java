package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class EmptyDeclaration extends PositionNode {
    public EmptyDeclaration (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName ();
    }
}