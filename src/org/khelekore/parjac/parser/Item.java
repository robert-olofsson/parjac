package org.khelekore.parjac.parser;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.SimplePart;

abstract class Item {
    private final Rule r;
    private final int dotPos;

    public Item (Rule r, int dotPos) {
	this.r = r;
	this.dotPos = dotPos;
    }

    @Override public String toString () {
	return "[" + r + ", dotPos: " + dotPos + "]";
    }

    public Rule getRule () {
	return r;
    }

    public int getDotPos () {
	return dotPos;
    }

    public SimplePart getPartAfterDot () {
	return r.getParts ().get (dotPos);
    }

    public boolean dotIsLast () {
	return dotPos == r.getParts ().size ();
    }
}
