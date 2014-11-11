package org.khelekore.parjac.parser;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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

    public Collection<Token> getPossibleNextTokens () {
	EnumSet<Token> nexts = EnumSet.noneOf (Token.class);
	for (Object o : actionPart.keySet ()) {
	    if (o instanceof Token)
		nexts.add ((Token)o);
	}
	return nexts;
    }

    @Override public String toString () {
	return stateId + ": " + actionPart;
    }
}
