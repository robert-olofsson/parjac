package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class UnaryExpression extends PositionNode {
    private final Token operator;
    private final TreeNode exp;

    public UnaryExpression (Token conversion, TreeNode exp, ParsePosition pos) {
	super (pos);
	this.operator = conversion;
	this.exp = exp;
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 1)
	    return parts.pop ();
	TreeNode conversionNode = parts.pop ();
	Token operator = ((OperatorTokenType)conversionNode).get ();
	TreeNode exp = parts.pop ();
	if (exp instanceof NumericValue) {
	    NumericValue lv = (NumericValue)exp;
	    if (operator == Token.MINUS)
		return lv.getNegated ();
	    // For plus we just return the actual value
	    return exp;
	}
	return new UnaryExpression (operator, exp, pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + operator + " " + exp + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	exp.visit (visitor);
    }

    public Token getOperator () {
	return operator;
    }

    @Override public ExpressionType getExpressionType () {
	return exp.getExpressionType ();
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (exp);
    }
}