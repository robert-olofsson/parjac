package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class LambdaExpression extends PositionNode {
    private final TreeNode parameters;
    private final TreeNode body;

    public LambdaExpression (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	parameters = parts.pop ();
	parts.pop (); // '->'
	body = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + parameters + " -> " + body + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (body);
    }
}