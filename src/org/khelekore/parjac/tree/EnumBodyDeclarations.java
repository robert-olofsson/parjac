package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class EnumBodyDeclarations implements TreeNode {
    private final List<TreeNode> classBodyDeclarations;

    public EnumBodyDeclarations (Rule r, Deque<TreeNode> parts) {
	classBodyDeclarations = r.size () > 1 ? ((ZOMEntry)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + classBodyDeclarations + "}";
    }

    public List<TreeNode> getDeclarations () {
	return classBodyDeclarations;
    }
}