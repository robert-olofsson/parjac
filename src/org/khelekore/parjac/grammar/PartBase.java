package org.khelekore.parjac.grammar;

import java.util.List;

abstract class PartBase<T> implements SimplePart, ComplexPart {
    protected final T data;

    public PartBase (T data) {
	this.data = data;
    }

    @Override final public String toString () {
	return data.toString ();
    }

    @Override final public int hashCode () {
	return data.hashCode ();
    }

    @SuppressWarnings ("unchecked") @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () == getClass ())
	    return data.equals (((PartBase<T>)o).data);
	return false;
    }

    final public T getId () {
	return data;
    }

    @Override public void split (List<List<SimplePart>> parts) {
	parts.forEach (ls -> ls.add (this));
    }
}
