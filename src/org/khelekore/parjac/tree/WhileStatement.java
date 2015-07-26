package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class WhileStatement extends PositionNode {
    private final TreeNode exp;
    private final TreeNode statement;

    public WhileStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	exp = parts.pop ();
	statement = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + " " + statement + "}";
    }
}
