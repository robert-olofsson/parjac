package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class Assignment extends PositionNode {
    private final TreeNode lhs;
    private final OperatorTokenType op;
    private final TreeNode rhs;

    public Assignment (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	lhs = parts.pop ();
	op = (OperatorTokenType)parts.pop ();
	rhs = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + lhs + " " + op + " " + rhs + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    public TreeNode lhs () {
	return lhs;
    }

    public OperatorTokenType getOperator () {
	return op;
    }

    public TreeNode rhs () {
	return rhs;
    }
}