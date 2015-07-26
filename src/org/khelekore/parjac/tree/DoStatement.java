package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class DoStatement extends PositionNode {
    private final TreeNode statement;
    private final TreeNode exp;

    public DoStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	statement = parts.pop ();
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + statement + " " + exp + "}";
    }
}
