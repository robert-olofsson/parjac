package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class WildcardBounds implements TreeNode {
    private final Token mode;
    private final TreeNode refType;

    public WildcardBounds (Rule r, Deque<TreeNode> parts) {
	mode = (Token)r.getRulePart (0).getId ();
	refType = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + mode + " " + refType + "}";
    }
}
