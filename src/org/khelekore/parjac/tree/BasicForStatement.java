package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class BasicForStatement implements TreeNode {
    private final TreeNode forInit;
    private final TreeNode exp;
    private final StatementExpressionList forUpdate;
    private final TreeNode statement;

    public BasicForStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	int pos = 2;
	if (r.getRulePart (pos).isRulePart ()) {
	    forInit = parts.pop ();
	    pos++;
	} else {
	    forInit = null;
	}
	pos++;
	if (r.getRulePart (pos).isRulePart ()) {
	    exp = parts.pop ();
	    pos++;
	} else {
	    exp = null;
	}
	pos++;
	if (r.getRulePart (pos).isRulePart ()) {
	    forUpdate = (StatementExpressionList)parts.pop ();
	    pos++;
	} else {
	    forUpdate = null;
	}
	statement = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{for (" + forInit + "; " +
	    exp + "; " + forUpdate + ") " + statement + "}";
    }
}
