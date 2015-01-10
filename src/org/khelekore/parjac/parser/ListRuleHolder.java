package org.khelekore.parjac.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.lexer.Token;

class ListRuleHolder {
    private Set<Rule> completed = Collections.emptySet ();

    // (Token | rulename) -> rules
    private final Map<Object, Set<Rule>> m = new HashMap<> ();

    ListRuleHolder (Collection<Rule> rr) {
	for (Rule r : rr) {
	    if (r.isEmpty ()) {
		if (completed.isEmpty ())
		    completed = new HashSet<> ();
		completed.add (r);
	    } else {
		SimplePart sp = r.getRulePart (0);
		Set<Rule> ls = m.get (sp.getId ());
		if (ls == null) {
		    ls = new HashSet<> ();
		    m.put (sp.getId (), ls);
		}
		ls.add (r);
	    }
	}
    }

    public void add (ListRuleHolder other) {
	if (completed.isEmpty () && !other.completed.isEmpty ()) {
	    completed = new HashSet<> ();
	    completed.addAll (other.completed);
	}

	for (Map.Entry<Object, Set<Rule>> me : other.m.entrySet ()) {
	    Object key = me.getKey ();
	    Set<Rule> ls = m.get (key);
	    if (ls == null) {
		m.put (key, new HashSet<> (me.getValue ()));
	    } else {
		ls.addAll (me.getValue ());
	    }
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" +
	    "completed: " + completed + ", m: " + m + "}";
    }

    public Set<String> getStartingRules () {
	Set<String> startingRules = new HashSet<> ();
	for (Object o : m.keySet ())
	    if (o instanceof String)
		startingRules.add ((String)o);
	return startingRules;
    }

    public Iterator<Rule> getCompletedRules () {
	return completed.iterator ();
    }

    public Iterator<Rule> getRulesWithRuleNext (Rule r) {
	Set<Rule> ls = m.get (r.getName ());
	if (ls == null)
	    return Collections.emptyIterator ();
	return ls.iterator ();
    }

    public Iterator<Rule> getRulesWithTokenNext(Token t) {
	Set<Rule> ls = m.get (t);
	if (ls == null)
	    return Collections.emptyIterator ();
	return ls.iterator ();
    }
}
