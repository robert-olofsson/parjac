package org.khelekore.parjac.tree;

import java.util.Objects;

public class ExpressionType {
    private final String className;
    private final boolean isPrimitive;

    public static final ExpressionType BYTE = new ExpressionType ("B", true);
    public static final ExpressionType SHORT = new ExpressionType ("S", true);
    public static final ExpressionType CHAR = new ExpressionType ("C", true);
    public static final ExpressionType INT = new ExpressionType ("I", true);
    public static final ExpressionType LONG = new ExpressionType ("J", true);
    public static final ExpressionType FLOAT = new ExpressionType ("F", true);
    public static final ExpressionType DOUBLE = new ExpressionType ("D", true);
    public static final ExpressionType BOOLEAN = new ExpressionType ("Z", true);

    public static final ExpressionType VOID = new ExpressionType ("V", false);
    public static final ExpressionType NULL = new ExpressionType ("null", false);

    public static final ExpressionType STRING = new ExpressionType ("java.lang.String", false);
    public static final ExpressionType OBJECT = new ExpressionType ("java.lang.OBJECT", false);

    /**
     * @param className fully qualified class name "foo.bar.Baz"
     */
    private ExpressionType (String className, boolean isPrimitive) {
	this.className = className;
	this.isPrimitive = isPrimitive;
    }

    /**
     * @param className fully qualified class name "foo.bar.Baz"
     */
    public ExpressionType (String className) {
	this (className, false);
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	ExpressionType e = (ExpressionType)o;
	return e.isPrimitive == isPrimitive &&
	    e.className.equals (className);
    }

    @Override public int hashCode () {
	return Objects.hash (className, isPrimitive);
    }

    @Override public String toString () {
	return className;
    }

    public boolean isPrimitiveType () {
	return isPrimitive;
    }

    public boolean isClassType () {
	return !isPrimitive;
    }

    public String getClassName () {
	if (isClassType ())
	    return className;
	throw new IllegalStateException ("Not a class type");
    }

    public String getPrimitiveName () {
	if (isPrimitiveType ())
	    return className;
	throw new IllegalStateException ("Not a primitive type");
    }
}
