package org.khelekore.parjac.parser;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.RulePart;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.grammar.TokenPart;
import org.khelekore.parjac.lexer.Token;

class SimpleMultiState implements MultiState {
    private final State s;
    private final String ruleName;
    private final Token t;

    public SimpleMultiState (State s) {
	this.s = s;
	if (s.dotIsLast ()) {
	    ruleName = null;
	    t = null;
	} else {
	    SimplePart sp = s.getPartAfterDot ();
	    if (sp instanceof TokenPart) {
		ruleName = null;
		t = ((TokenPart)sp).getId ();
	    } else {
		ruleName = ((RulePart)sp).getId ();
		t = null;
	    }
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{s: " + s + ", r: " + ruleName + ", t: " + t + "}";
    }

    public Iterator<State> getCompletedStates () {
	if (s.dotIsLast ())
	    return Collections.singleton (s).iterator ();
	return Collections.emptyIterator ();
    }

    public Set<String> getPredictRules () {
	if (ruleName != null)
	    return Collections.singleton (ruleName);
	return Collections.emptySet ();
    }

    public Iterator<State> getRulesWithNext (Rule r) {
	if (this.ruleName.equals (r.getName ()))
	    return Collections.singleton (s).iterator ();
	return Collections.emptyIterator ();
    }

    public Iterator<State> getRulesWithNext (Token t) {
	if (this.t == t)
	    return Collections.singleton (s).iterator ();
	return Collections.emptyIterator ();
    }
}
