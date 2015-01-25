package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class LocalVariableDeclarationStatement implements TreeNode {
    private final LocalVariableDeclaration lvd;

    public LocalVariableDeclarationStatement (Rule r, Deque<TreeNode> parts) {
	lvd = (LocalVariableDeclaration)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + lvd + ";}";
    }
}
