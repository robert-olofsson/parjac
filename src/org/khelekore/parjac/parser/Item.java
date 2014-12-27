package org.khelekore.parjac.parser;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.SimplePart;

class Item {
    private final Rule r;
    private final int dotPos;
    private final int hc;

    public Item (Rule r, int dotPos) {
	this.r = r;
	this.dotPos = dotPos;
	hc = r.hashCode () + dotPos;
    }

    @Override public String toString () {
	return "[" + r + ", dotPos: " + dotPos + "]";
    }

    @Override public int hashCode () {
	return hc;
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	Item i = (Item)o;
	return dotPos == i.dotPos && r.equals (i.r);
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

    public Item advance () {
	return new Item (r, dotPos + 1);
    }

    public boolean dotIsLast () {
	return dotPos == r.getParts ().size ();
    }
}
