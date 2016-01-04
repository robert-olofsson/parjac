package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class PostIncrementExpression extends PositionNode {
    private final TreeNode exp;

    public PostIncrementExpression (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	exp = parts.pop ();
	parts.pop (); // '++'
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    exp.visit (visitor);
    }

    @Override public ExpressionType getExpressionType () {
	return exp.getExpressionType ();
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singletonList (exp);
    }
}
