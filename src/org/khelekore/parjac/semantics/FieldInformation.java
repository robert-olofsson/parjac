package org.khelekore.parjac.semantics;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.FlaggedType;
import org.khelekore.parjac.tree.TreeNode;

public class FieldInformation<T extends FlaggedType> {
    private final String name;
    private final T var;
    private final TreeNode owner;

    public FieldInformation (String name, T var, TreeNode owner) {
	this.name = name;
	this.var = var;
	this.owner = owner;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + name + ", var: " + var +
	    ", owner: " + owner.getClass ().getName () + "}";
    }

    public String getName () {
	return name;
    }

    public TreeNode  getOwner () {
	return owner;
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

    public ExpressionType getExpressionType () {
	return var.getExpressionType ();
    }
}
