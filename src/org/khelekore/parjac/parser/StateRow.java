package org.khelekore.parjac.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.khelekore.parjac.lexer.Token;

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
	Action previous = actionPart.put (t, a);
	if (previous != null) {
	    System.out.println (stateId + ": overwrote: " + t + " -> " + previous + ", with: "  + a);
	}
    }

    public Action getAction (Object t) {
	return actionPart.get (t);
    }

    public Collection<Object> getPossibleNextTokens () {
	return actionPart.keySet ().stream ().filter (o -> o instanceof Token).collect (Collectors.toSet ());
    }

    @Override public String toString () {
	return stateId + ": " + actionPart;
    }
}
