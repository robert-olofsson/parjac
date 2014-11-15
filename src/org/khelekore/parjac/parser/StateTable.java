package org.khelekore.parjac.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.khelekore.parjac.lexer.Token;

public class StateTable {
    private final List<StateRow> states = new ArrayList<> ();

    public int size () {
	return states.size ();
    }

    public void addState (StateRow row) {
	states.add (row);
    }

    public StateRow get (int row) {
	return states.get (row);
    }

    public Action getAction (int state, Token token) {
	StateRow row = states.get (state);
	return row.getAction (token);
    }

    public int getGoTo (int state, String rule) {
	StateRow row = states.get (state);
	return row.getAction (rule).getN ();
    }

    public Collection<Token> getPossibleNextTokens (int state) {
	StateRow row = states.get (state);
	return row.getPossibleNextTokens ();
    }

    public String toTableString () {
	StringBuilder sb = new StringBuilder ();
	states.forEach (sr -> sb.append (sr).append ("\n"));
	return sb.toString ();
    }
}