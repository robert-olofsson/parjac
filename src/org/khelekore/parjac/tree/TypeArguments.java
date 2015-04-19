package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class TypeArguments implements TreeNode {
    private final List<TreeNode> typeArgumentList;

    public TypeArguments (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	parts.pop (); // '<'
	typeArgumentList = ((TypeArgumentList)parts.pop ()).get ();
	parts.pop (); // '>'
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + typeArgumentList + "}";
    }

    public List<TreeNode> getTypeArguments () {
	return typeArgumentList;
    }
}
