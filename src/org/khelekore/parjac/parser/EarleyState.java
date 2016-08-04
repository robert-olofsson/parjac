package org.khelekore.parjac.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.TreeNode;

public class EarleyState {

    private ParsePosition parsePosition;
    private final TreeNode tokenValue;
    // Predicted rules
    private ListRuleHolder lrh;
    // States that have been advanced in some way
    private Set<State> states = Collections.emptySet ();
    private boolean cleared = false; // have we removed non-used states?

    public EarleyState (TreeNode tokenValue) {
	this.tokenValue = tokenValue;
    }

    public void setParsePosition (ParsePosition parsePosition) {
	this.parsePosition = parsePosition;
    }

    public ParsePosition getParsePosition () {
	return parsePosition;
    }

    public TreeNode getTokenValue () {
	return tokenValue;
    }

    public void addStates (Collection<State> states) {
	if (states.isEmpty ())
	    states = new LinkedHashSet<> ();
	this.states.addAll (states);
    }

    public void addState (State state) {
	if (states.isEmpty ())
	    states = new LinkedHashSet<> ();
	states.add (state);
    }

    public Set<State> getStates () {
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






