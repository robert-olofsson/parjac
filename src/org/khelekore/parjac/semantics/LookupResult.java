package org.khelekore.parjac.semantics;

class LookupResult {
    private boolean found;
    private int accessFlags;

    public static final LookupResult NOT_FOUND = new LookupResult (false, 0);

    public LookupResult (boolean found, int accessFlags) {
	this.found = found;
	this.accessFlags = accessFlags;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{found: " + found + ", flags: " + accessFlags + "}";
    }

    public boolean getFound () {
	return found;
    }

    public int getAccessFlags () {
	return accessFlags;
    }
}