package org.khelekore.parjac.parser;

import java.util.Iterator;
import java.util.Set;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public interface MultiState {
    Iterator<State> getCompletedStates ();
    Set<String> getPredictRules ();
    Iterator<State> getRulesWithNext (Rule r);
    Iterator<State> getRulesWithNext (Token t);
}
