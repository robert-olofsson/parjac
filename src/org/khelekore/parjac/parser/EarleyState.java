package org.khelekore.parjac.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.TreeNode;

public class EarleyState {

    private final ParsePosition parsePosition;
    private final TreeNode tokenValue;
    // Predicted rules
    private ListRuleHolder lrh;
    // States that have been advanced in some way
    private List<State> states = Collections.emptyList ();
    private boolean cleared = false; // have we removed non-used states?

    public EarleyState (ParsePosition parsePosition, TreeNode tokenValue) {
	this.parsePosition = parsePosition;
	this.tokenValue = tokenValue;
    }

    public ParsePosition getParsePosition () {
	return parsePosition;
    }

    public TreeNode getTokenValue () {
	return tokenValue;
    }

    public void addStates (List<State> states) {
	for (State s : states)
	    addState (s);
    }

    public void addState (State state) {
	if (states.isEmpty ())
	    states = new ArrayList<> ();
	for (State s : states)
	    if (s.equals (state))
		return;
	states.add (state);
    }

    public List<State> getStates () {
	return states;
    }

    public ListRuleHolder getListRuleHolder () {
	return lrh;
    }

    public void setPredictedStates (ListRuleHolder lrh) {
	this.lrh = lrh;
    }

    public EnumSet<Token> getPossibleNextToken () {
	EnumSet<Token> et = EnumSet.noneOf (Token.class);
	if (lrh != null) {
	    et.addAll (lrh.getStartingTokens ());
	    for (State s : states) {
		if (!s.dotIsLast ()) {
		    SimplePart sp = s.getPartAfterDot ();
		    if (sp.isTokenPart ())
			et.add ((Token)sp.getId ());
		}
	    }
	}
	return et;
    }

    public boolean isEmpty () {
	return states.isEmpty () && lrh == null;
    }

    public boolean hasBeenCleared () {
	return cleared;
    }

    /** Mark this state as cleared
     */
    public void setCleared () {
	cleared = true;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" +
	    "parsePosition: " + parsePosition +
	    ", tokenValue: " + tokenValue +
	    ", lrh: " + lrh +
	    ", states: " + states +
	    "}";
    }
}






