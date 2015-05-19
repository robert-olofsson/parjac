package org.khelekore.parjac.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.SimplePart;

class State {
    private final Rule r;
    private final int positions; // 24 bit startpos, 8 bit dotPos.

    public State (Rule r, int dotPos, int startPos) {
	this.r = r;
	this.positions = (0xff & dotPos) | (startPos << 8);
    }

    public Rule getRule () {
	return r;
    }

    public int getDotPos () {
	return positions & 0xff;
    }

    public SimplePart getPartAfterDot () {
	return r.getParts ().get (getDotPos ());
    }

    public boolean dotIsLast () {
	return getDotPos () == r.getParts ().size ();
    }

    public int getStartPos () {
	return positions >>> 8;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () +
	    "{" + getRule () + ", " + getDotPos () + ", " + getStartPos () + "}";
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	State s = (State)o;
	return positions == s.positions &&
	    getRule () == s.getRule (); // yes, ref comparisson
    }

    @Override public int hashCode () {
	return positions | r.getId ();
    }

    public State advance (State completed) {
	return new StateWithPrevious (getRule (), getDotPos () + 1, getStartPos (), this, completed);
    }

    public State getPrevious () {
	return null;
    }

    public void addCompleted (State c) {
	throw new IllegalStateException ("Can not add completed to: " + this);
    }

    public List<State> getCompleted () {
	return null;
    }
}

class StateWithPrevious extends State {
    private State previousState;
    private List<State> completed;

    public StateWithPrevious (Rule r, int dotPos, int startPos, State previousState, State completed) {
	super (r, dotPos, startPos);
	this.previousState = previousState;
	if (completed != null)
	    this.completed = Collections.singletonList (completed);
    }

    @Override public State getPrevious () {
	return previousState;
    }

    @Override public void addCompleted (State c) {
	if (completed.size () == 1) {
	    List<State> ls = new ArrayList<> ();
	    ls.add (completed.get (0));
	    completed = ls;
	}
	completed.add (c);
    }

    @Override public List<State> getCompleted () {
	return completed;
    }
}
