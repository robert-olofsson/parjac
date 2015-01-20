package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class EnumBodyDeclarations implements TreeNode {
    private final List<TreeNode> classBodyDeclarations;

    public EnumBodyDeclarations (Rule r, Deque<TreeNode> parts) {
	ZOMEntry ze = (ZOMEntry)parts.pop ();
	classBodyDeclarations = ze.get ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + classBodyDeclarations + "}";
    }
}