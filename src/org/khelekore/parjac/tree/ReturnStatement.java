package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ReturnStatement extends PositionNode {
    private final TreeNode exp;

    public ReturnStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	exp = r.size () > 2 ? parts.pop () : null;
    }

    /** Create a void return */
    public ReturnStatement (ParsePosition pos) {
	super (pos);
	exp = null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    if (exp != null) {
		exp.visit (visitor);
	    }
	}
	visitor.endReturn (this);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	if (exp == null)
	    return Collections.emptyList ();
	return Collections.singleton (exp);
    }

    public boolean hasExpression () {
	return exp != null;
    }

    public TreeNode getExpression () {
	return exp;
    }

    @Override public ExpressionType getExpressionType () {
	if (exp != null)
	    return exp.getExpressionType ();
	return ExpressionType.VOID;
    }
}