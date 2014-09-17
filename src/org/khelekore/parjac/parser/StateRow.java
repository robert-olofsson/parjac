package org.khelekore.parjac.parser;

import java.util.Map;

import org.khelekore.parjac.lexer.Token;

public class StateRow {
    private final int stateId;
    private final Map<Token, Action> actionPart = null;
    private final Map<String, Integer> goToPart = null;

    public StateRow (int stateId) {
	this.stateId = stateId;
    }

    public Action getAction (Token token) {
	return actionPart.get (token);
    }

    public Integer getGoTo (String rule) {
	return goToPart.get (rule);
    }
}
