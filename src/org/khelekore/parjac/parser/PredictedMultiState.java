package org.khelekore.parjac.parser;

import java.util.Iterator;
import java.util.Set;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.TreeNode;

class PredictedMultiState implements MultiState {
    private final ListRuleHolder rules;
    private final int startPos;

    public PredictedMultiState (ListRuleHolder rules, int startPos) {
	this.rules = rules;
	this.startPos = startPos;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" +
	    "rules: " + rules + ", startPos: " + startPos + "}";
    }

    public Iterator<State> getCompletedStates () {
	return new PSSIterator (rules.getCompletedRules ());
    }

    public Set<String> getPredictRules () {
	return rules.getStartingRules ();
    }

    public Iterator<State> getRulesWithNext (Rule r) {
	return new PSSIterator (rules.getRulesWithRuleNext (r));
    }

    public Iterator<State> getRulesWithNext (Token t) {
	return new PSSIterator (rules.getRulesWithTokenNext (t));
    }

    public TreeNode getParsedToken () {
	return null;
    }

    class PSSIterator implements Iterator<State> {
	Iterator<Rule> innerIterator;
	PSSIterator (Iterator<Rule> rules) {
	    innerIterator = rules;
	}

	public boolean hasNext () {
	    return innerIterator.hasNext ();
	}

	public State next () {
	    Rule r = innerIterator.next ();
	    return new State (r, 0, startPos);
	}
    }
}
