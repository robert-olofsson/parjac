package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class CastExpression extends PositionNode {
    private final TreeNode type;
    private final List<TreeNode> additionalBounds;
    private final TreeNode expression;

    public CastExpression (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	type = parts.pop ();
	additionalBounds = r.size () > 4 ? ((ZOMEntry)parts.pop ()).get () : null;
	expression = parts.pop ();
    }

    public CastExpression (TreeNode type, TreeNode expression, ParsePosition pos) {
	super (pos);
	this.type = type;
	this.additionalBounds = null;
	this.expression = expression;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + additionalBounds + " " + expression + "}";
    }

    @Override public ExpressionType getExpressionType () {
	return type.getExpressionType ();
    }

    public TreeNode getType () {
	return type;
    }

    public TreeNode getExpression () {
	return expression;
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    expression.visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (expression);
    }
}
