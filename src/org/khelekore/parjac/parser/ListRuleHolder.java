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

    private ListRuleHolder () {
	// hidden
    }

    ListRuleHolder (Collection<Rule> rr) {
	for (Rule r : rr) {
	    if (r.size () == 0) {
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

    public ListRuleHolder merge (ListRuleHolder other) {
	if (other == null)
	    return this;
	ListRuleHolder ret = new ListRuleHolder ();
	if (!completed.isEmpty () || !other.completed.isEmpty ()) {
	    ret.completed = new HashSet<> ();
	    ret.completed.addAll (completed);
	    ret.completed.addAll (other.completed);
	}
	ret.m.putAll (m);
	for (Map.Entry<Object, Set<Rule>> me : other.m.entrySet ()) {
	    Set<Rule> ls = ret.m.get (me.getKey ());
	    if (ls == null) {
		ret.m.put (me.getKey (), me.getValue ());
	    } else {
		ls.addAll (me.getValue ());
	    }
	}
	return ret;
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
