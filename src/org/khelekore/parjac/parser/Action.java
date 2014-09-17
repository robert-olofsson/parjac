package org.khelekore.parjac.parser;

public class Action {
    private final Type t;
    private final int n;

    public enum Type { SHIFT, REDUCE, ACCEPT, ERROR };

    private Action (Type t, int n) {
	this.t = t;
	this.n = n;
    }

    public Type getType () {
	return t;
    }

    public int getN () {
	return n;
    }

    public Action createShift (int n) {
	return new Action (Type.SHIFT, n);
    }

    public Action createReduce (int n) {
	return new Action (Type.REDUCE, n);
    }

    public Action createAccept () {
	return new Action (Type.ACCEPT, -1);
    }

    public Action createError () {
	return new Action (Type.ERROR, -1);
    }
}