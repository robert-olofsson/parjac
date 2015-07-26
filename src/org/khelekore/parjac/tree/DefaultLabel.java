package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public class DefaultLabel extends PositionNode {
    public DefaultLabel (ParsePosition pos) {
	super (pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{}";
    }
}