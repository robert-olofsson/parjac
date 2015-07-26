package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class TwoPartExpression extends PositionNode {
    private final TreeNode exp1;
    private final OperatorTokenType op;
    private final TreeNode exp2;

    public TwoPartExpression (TreeNode exp1, OperatorTokenType op, TreeNode exp2, ParsePosition pos) {
	super (pos);
	this.exp1 = exp1;
	this.op = op;
	this.exp2 = exp2;
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 1)
	    return parts.pop ();
	return new TwoPartExpression (parts.pop (), (OperatorTokenType)parts.pop (), parts.pop (), pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp1 + " " + op + " " + exp2 + "}";
    }
}