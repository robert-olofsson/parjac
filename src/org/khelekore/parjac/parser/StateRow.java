package org.khelekore.parjac.parser;

import java.util.HashMap;
import java.util.Map;

public class StateRow {
    private final int stateId;
    private final Map<Object, Action> actionPart = new HashMap<> ();

    public StateRow (int stateId) {
	this.stateId = stateId;
    }

    public int getId () {
	return stateId;
    }

    public void addAction (Object t, Action a) {
	Object previous = actionPart.put (t, a);
	if (previous != null)
	    System.out.println (stateId + ": overwrote: " + t + " -> " + previous + ", with: "  + a);
    }

    public Action getAction (Object t) {
	return actionPart.get (t);
    }

    @Override public String toString () {
	return stateId + ": " + actionPart;
    }
}
