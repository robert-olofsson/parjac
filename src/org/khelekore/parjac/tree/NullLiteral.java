package org.khelekore.parjac.tree;

public class NullLiteral implements TreeNode {
    public static final NullLiteral NULL = new NullLiteral ();

    private NullLiteral () {
	// empty
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }
}