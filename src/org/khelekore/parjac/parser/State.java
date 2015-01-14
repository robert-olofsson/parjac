package org.khelekore.parjac.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public State advance (State completed) {
	return new StateWithPrevious (getRule (), getDotPos () + 1, startPos, this, completed);
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
