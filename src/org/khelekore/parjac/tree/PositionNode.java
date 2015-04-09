package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public abstract class PositionNode implements TreeNode {
    private final ParsePosition pos;

    public PositionNode (ParsePosition pos) {
	this.pos = pos;
    }

    public ParsePosition getParsePosition () {
	return pos;
    }
}