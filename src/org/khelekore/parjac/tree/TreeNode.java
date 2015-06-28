package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public interface TreeNode {
    /** Handle tree visitors */
    default void visit (TreeVisitor visitor) {
	// empty
    }

    default ParsePosition getParsePosition () {
	throw new RuntimeException ("No parse position for: " + getClass ().getName ());
    }
}