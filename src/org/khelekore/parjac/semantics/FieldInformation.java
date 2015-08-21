package org.khelekore.parjac.semantics;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.FlaggedType;

public class FieldInformation<T extends FlaggedType> {
    private final String name;
    private final T var;
    private final int classLevel;

    public FieldInformation (String name, T var, int classLevel) {
	this.name = name;
	this.var = var;
	this.classLevel = classLevel;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + name + ", var: " + var + "}";
    }

    public String getName () {
	return name;
    }

    public int getClassLevel () {
	return classLevel;
    }

    public T getVariableDeclaration () {
	return var;
    }

    public ParsePosition getParsePosition () {
	return var.getParsePosition ();
    }

    public boolean isStatic () {
	return FlagsHelper.isStatic (var.getFlags ());
    }

    public String getExpressionType () {
	return var.getExpressionType ();
    }
}
