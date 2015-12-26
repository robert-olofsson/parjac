package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;

import org.khelekore.parjac.lexer.ParsePosition;

public interface TreeNode {
    /** Visit this node and its child nodes */
    default void visit (TreeVisitor visitor) {
	// empty
    }

    /** Visit this node */
    default void simpleVisit (TreeVisitor visitor) {
	// empty
    }

    /** Get the parse position for this node */
    ParsePosition getParsePosition ();

    /** Get the expression type for this node */
    default ExpressionType getExpressionType () {
	return null;
    }

    /** Get the child nodes for this nodes. Use empty collection for no childs.
     */
    default Collection<? extends TreeNode> getChildNodes () {
	return Collections.emptyList ();
    }
}