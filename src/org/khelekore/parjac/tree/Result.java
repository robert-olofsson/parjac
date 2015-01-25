package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class Result implements TreeNode {
    private final TreeNode type;

    public Result (Rule r, Deque<TreeNode> parts) {
	type = r.getRulePart (0).getId () == Token.VOID ? null : parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + "}";
    }
}
