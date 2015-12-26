package org.khelekore.parjac.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class Assignment extends PositionNode {
    private TreeNode lhs;
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
	lhs.visit (visitor);
	rhs.visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Arrays.asList (lhs, rhs);
    }

    public TreeNode lhs () {
	return lhs;
    }

    /** Replace the left hand side, typically done when replacing dotted name */
    public void lhs (TreeNode lhs) {
	this.lhs = lhs;
    }

    public OperatorTokenType getOperator () {
	return op;
    }

    public TreeNode rhs () {
	return rhs;
    }
}