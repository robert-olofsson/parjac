package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class BasicForStatement extends PositionNode {
    private final TreeNode forInit;
    private final TreeNode exp;
    private final StatementExpressionList forUpdate;
    private final TreeNode statement;

    public BasicForStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
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

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    if (forInit != null)
		forInit.visit (visitor);
	    if (exp != null)
		exp.visit (visitor);
	    if (forUpdate != null)
		forUpdate.visit (visitor);
	    statement.visit (visitor);
	}
	visitor.endFor ();
    }

    public TreeNode getForInit () {
	return forInit;
    }

    public TreeNode getExpression () {
	return exp;
    }

    public StatementExpressionList getForUpdate () {
	return forUpdate;
    }

    public TreeNode getStatement () {
	return statement;
    }
}
