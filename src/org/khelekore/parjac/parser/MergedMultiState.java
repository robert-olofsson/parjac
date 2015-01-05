package org.khelekore.parjac.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

class MergedMultiState implements MultiState {

    private final MultiState m1;
    private final MultiState m2;
    private final MultiState m3;

    public static MultiState get (MultiState m1, MultiState m2, MultiState m3) {
	if (m1 == null)
	    throw new NullPointerException ("Did not expect m1 to be null");
	if (m2 == null && m3 == null)
	    return m1;
	return new MergedMultiState (m1, m2, m3);
    }

    private MergedMultiState (MultiState m1, MultiState m2, MultiState m3) {
	this.m1 = m1;
	this.m2 = m2;
	this.m3 = m3;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" +
	    "m1: " + m1 + ", m2: " + m2 + ", m3: " + m3 + "}";
    }

    public Iterator<State> getCompletedStates () {
	return new MultiIterator<> (getCompletedStates (m1),
				    getCompletedStates (m2),
				    getCompletedStates (m3));
    }

    private Iterator<State> getCompletedStates (MultiState ms) {
	if (ms == null)
	    return Collections.emptyIterator ();
	return ms.getCompletedStates ();
    }

    public Set<String> getPredictRules () {
	Set<String> ret = new HashSet<> ();
	ret.addAll (getPredictRules (m1));
	ret.addAll (getPredictRules (m2));
	ret.addAll (getPredictRules (m3));
	return ret;
    }

    private Set<String> getPredictRules (MultiState ms) {
	if (ms == null)
	    return Collections.emptySet ();
	return ms.getPredictRules ();
    }

    public Iterator<State> getRulesWithNext (Rule r) {
	return new MultiIterator<> (getRulesWithNext (m1, r),
				    getRulesWithNext (m2, r),
				    getRulesWithNext (m3, r));
    }

    private Iterator<State> getRulesWithNext (MultiState ms, Rule r) {
	if (ms == null)
	    return Collections.emptyIterator ();
	return ms.getRulesWithNext (r);
    }

    public Iterator<State> getRulesWithNext (Token t) {
	return new MultiIterator<> (getRulesWithNext (m1, t),
				    getRulesWithNext (m2, t),
				    getRulesWithNext (m3, t));
    }

    private Iterator<State> getRulesWithNext (MultiState ms, Token t) {
	if (ms == null)
	    return Collections.emptyIterator ();
	return ms.getRulesWithNext (t);
    }

    private static class MultiIterator<T> implements Iterator<T> {
	private Iterator<Iterator<T>> is;
	private Iterator<T> current;

	public MultiIterator (Iterator<T> i1, Iterator<T> i2, Iterator<T> i3) {
	    is = Arrays.asList (i1, i2, i3).iterator ();
	    current = is.next ();
	    while (!current.hasNext () && is.hasNext ())
		current = is.next ();
	}

	public boolean hasNext () {
	    if (current.hasNext ())
		return true;
	    while (is.hasNext ()) {
		current = is.next ();
		if (current.hasNext ())
		    return true;
	    }
	    return current.hasNext ();
	}

	public T next () {
	    return current.next ();
	}
    }
}