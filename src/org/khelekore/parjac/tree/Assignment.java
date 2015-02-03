package org.khelekore.parjac.tree;

import java.util.Deque;

public class Assignment implements TreeNode {
    private final TreeNode lhs;
    private final OperatorTokenType op;
    private final TreeNode rhs;

    public Assignment (Deque<TreeNode> parts) {
	lhs = parts.pop ();
	op = (OperatorTokenType)parts.pop ();
	rhs = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + lhs + " " + op + " " + rhs + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	rhs.visit (visitor);
    }
}