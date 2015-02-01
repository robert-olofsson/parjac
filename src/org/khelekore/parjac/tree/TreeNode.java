package org.khelekore.parjac.tree;

public interface TreeNode {
    /** Handle tree visitors */
    public default void visit (TreeVisitor visitor) {
	// empty
    }
}