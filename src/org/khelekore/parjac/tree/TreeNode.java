package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public interface TreeNode {
    /** Handle tree visitors */
    default void visit (TreeVisitor visitor) {
	// empty
    }

    /** Get the parse position for this node */
    ParsePosition getParsePosition ();

    default String getExpressionType () {
	return null;
    }
}