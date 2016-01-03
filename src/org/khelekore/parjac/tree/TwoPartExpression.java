package org.khelekore.parjac.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

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

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    exp1.visit (visitor);
	    exp2.visit (visitor);
	}
    }

    @Override public ExpressionType getExpressionType () {
	if (op.get ().isLogicalOperator ())
	    return ExpressionType.BOOLEAN;
	return ExpressionType.bigger (exp1.getExpressionType (), exp2.getExpressionType ());
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	return Arrays.asList (exp1, exp2);
    }

    public boolean hasPrimitiveParts () {
	return exp1.getExpressionType ().isPrimitiveType ();
    }

    public Token getOperator () {
	return op.get ();
    }
}