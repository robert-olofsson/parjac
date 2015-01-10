package org.khelekore.parjac.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.RulePart;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.lexer.Token;

class ListMultiState implements MultiState {
    private List<State> completed = Collections.emptyList ();
    private final Map<Object, List<State>> m = new HashMap<> ();
    private Set<String> predictRules = Collections.emptySet ();

    public ListMultiState (List<State> ss) {
	for (State s : ss) {
	    if (s.dotIsLast ()) {
		if (completed.isEmpty ())
		    completed = new ArrayList<> ();
		completed.add (s);
	    } else {
		SimplePart sp = s.getPartAfterDot ();
		List<State> ls = m.get (sp.getId ());
		if (ls == null) {
		    ls = new ArrayList<> ();
		    m.put (sp.getId (), ls);
		}
		ls.add (s);
		if (sp instanceof RulePart) {
		    if (predictRules.isEmpty ())
			predictRules = new HashSet<> ();
		    predictRules.add ((String)sp.getId ());
		}
	    }
	}
    }

    public List<State> getCompleted () {
	return completed;
    }

    public boolean hasNonCompletedStates () {
	return !m.isEmpty ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" +
	    "completed: " + completed + ", m: " + m + "}";
    }

    public Iterator<State> getCompletedStates () {
	return completed.iterator ();
    }

    public Set<String> getPredictRules () {
	return predictRules;
    }

    public Iterator<State> getRulesWithNext (Rule r) {
	List<State> ls = m.get (r.getName ());
	if (ls == null)
	    return Collections.emptyIterator ();
	return ls.iterator ();
    }

    public Iterator<State> getRulesWithNext (Token t) {
	List<State> ls = m.get (t);
	if (ls == null)
	    return Collections.emptyIterator ();
	return ls.iterator ();
    }
}