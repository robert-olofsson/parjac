package org.khelekore.parjac.parser;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac.lexer.Token;

public class StateTable {
    private final List<StateRow> states = new ArrayList<> ();

    public StateTable () {
    }

    public void addState (StateRow row) {
	states.add (row);
    }

    public Action getAction (int state, Token token) {
	StateRow row = states.get (state);
	return row.getAction (token);
    }

    public Integer getGoTo (int state, String rule) {
	StateRow row = states.get (state);
	return row.getGoTo (rule);
    }
}