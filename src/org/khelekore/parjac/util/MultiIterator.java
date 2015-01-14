package org.khelekore.parjac.util;

import java.util.Arrays;
import java.util.Iterator;

public class MultiIterator<T> implements Iterator<T> {
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
