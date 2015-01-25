package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class MethodBody implements TreeNode {
    private final Block block;

    public MethodBody (Rule r, Deque<TreeNode> parts) {
	block = r.getRulePart (0).getId () == Token.SEMICOLON ? null : (Block)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + block + "}";
    }
}
