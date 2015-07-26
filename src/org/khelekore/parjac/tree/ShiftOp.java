package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class ShiftOp extends PositionNode {

    private ShiftOp (ParsePosition pos) {
	// do not create
	super (pos);
    }

    public static TreeNode create (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	OperatorTokenType first = (OperatorTokenType)parts.pop ();
	if (r.size () == 1)
	    return first;
	if (r.size () == 2) {
	    parts.pop ();
	    return new OperatorTokenType (Token.RIGHT_SHIFT, first.getParsePosition ());
	} else {
	    parts.pop ();
	    parts.pop ();
	    return new OperatorTokenType (Token.RIGHT_SHIFT_UNSIGNED, first.getParsePosition ());
	}
    }
}