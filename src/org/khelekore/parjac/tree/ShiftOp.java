package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class ShiftOp implements TreeNode {

    private static final OperatorTokenType RIGHT_SHIIFT =
	new OperatorTokenType (Token.RIGHT_SHIFT);
    private static final OperatorTokenType RIGHT_SHIIFT_UNSIGNED =
	new OperatorTokenType (Token.RIGHT_SHIFT_UNSIGNED);

    private ShiftOp () {
	// do not create
    }

    public static TreeNode create (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    return parts.pop ();
	if (r.size () == 2) {
	    parts.pop ();
	    parts.pop ();
	    return RIGHT_SHIIFT;
	} else {
	    parts.pop ();
	    parts.pop ();
	    parts.pop ();
	    return RIGHT_SHIIFT_UNSIGNED;
	}
    }
}