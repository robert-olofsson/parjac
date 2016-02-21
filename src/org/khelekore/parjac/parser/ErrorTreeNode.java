package org.khelekore.parjac.parser;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.PositionNode;

/** Tree node that signals error recovery in the parser */
public  class ErrorTreeNode extends PositionNode {

    public ErrorTreeNode (ParsePosition pos) {
	super (pos);
    }
}
