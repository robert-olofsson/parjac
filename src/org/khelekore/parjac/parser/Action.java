package org.khelekore.parjac.parser;

public class Action {
    private final Type t;
    private final int n;

    public enum Type { SHIFT, REDUCE, ACCEPT, ERROR };

    private Action (Type t, int n) {
	this.t = t;
	this.n = n;
    }

    @Override public String toString () {
	if (t == Type.SHIFT || t == Type.REDUCE)
	    return t.toString () + n;
	return t.toString ();
    }

    public Type getType () {
	return t;
    }

    public int getN () {
	return n;
    }

    public static Action createShift (int n) {
	return new Action (Type.SHIFT, n);
    }

    public static Action createReduce (int n) {
	return new Action (Type.REDUCE, n);
    }

    public static Action createAccept () {
	return new Action (Type.ACCEPT, -1);
    }

    public static Action createError () {
	return new Action (Type.ERROR, -1);
    }
}