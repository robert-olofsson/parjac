package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class AssertStatement implements TreeNode {
    private final TreeNode expression1; // the boolean/Boolean expression
    private final TreeNode expression2; // may not be void

    public AssertStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	expression1 = parts.pop ();
	if (r.size () > 3) {
	    parts.pop (); // ':'
	    expression2 =  parts.pop ();
	} else {
	    expression2 =  null;
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + expression1 + " : " + expression2 + "}";
    }
}
