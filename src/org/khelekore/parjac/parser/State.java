package org.khelekore.parjac.parser;

import org.khelekore.parjac.grammar.Rule;

class State extends Item {
    private final int startPos;
    private final int hc;

    public State (Rule r, int dotPos, int startPos) {
	super (r, dotPos);
	this.startPos = startPos;
	hc = startPos * 31 + super.hashCode ();
    }

    public int getStartPos () {
	return startPos;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () +
	    "{" + getRule () + ", " + getDotPos () + ", " + startPos + "}";
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	State s = (State)o;
	return startPos == s.startPos &&
	    getDotPos () == s.getDotPos () &&
	    getRule () == s.getRule (); // yes, ref comparisson
    }

    @Override public int hashCode () {
	return hc;
    }

    @Override public State advance () {
	return new State (getRule (), getDotPos () + 1, startPos);
    }
}
