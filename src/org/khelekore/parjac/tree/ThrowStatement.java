package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class ThrowStatement extends PositionNode {
    private final TreeNode exp;

    public ThrowStatement (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	exp = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	exp.visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (exp);
    }
}
