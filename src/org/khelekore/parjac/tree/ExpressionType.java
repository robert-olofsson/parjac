package org.khelekore.parjac.tree;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.Type;

public class ExpressionType {
    private final String className;
    private final String slashName;
    private final boolean isPrimitive;
    private final int type;

    public static final ExpressionType BYTE = new ExpressionType ("B", true, Type.BYTE);
    public static final ExpressionType SHORT = new ExpressionType ("S", true, Type.SHORT);
    public static final ExpressionType CHAR = new ExpressionType ("C", true, Type.CHAR);
    public static final ExpressionType INT = new ExpressionType ("I", true, Type.INT);
    public static final ExpressionType LONG = new ExpressionType ("J", true, Type.LONG);
    public static final ExpressionType FLOAT = new ExpressionType ("F", true, Type.FLOAT);
    public static final ExpressionType DOUBLE = new ExpressionType ("D", true, Type.DOUBLE);
    public static final ExpressionType BOOLEAN = new ExpressionType ("Z", true, Type.BOOLEAN);

    public static final ExpressionType VOID = new ExpressionType ("V", true, Type.VOID);
    public static final ExpressionType NULL = new ExpressionType ("null", false, -1);

    public static final ExpressionType STRING = new ExpressionType ("java.lang.String", false, Type.OBJECT);
    public static final ExpressionType OBJECT = new ExpressionType ("java.lang.Object", false, Type.OBJECT);

    private static final ExpressionType[] primitives =
    { BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE, BOOLEAN, VOID };

    private final static Map<ExpressionType, List<ExpressionType>> ALLOWED_UPCASTS = new HashMap<> ();
    static {
	ALLOWED_UPCASTS.put (BYTE, Arrays.asList (SHORT, INT, LONG));
	ALLOWED_UPCASTS.put (SHORT, Arrays.asList (INT, LONG));
	ALLOWED_UPCASTS.put (CHAR, Arrays.asList (INT, LONG));
	ALLOWED_UPCASTS.put (INT, Arrays.asList (LONG));
	ALLOWED_UPCASTS.put (FLOAT, Arrays.asList (DOUBLE));
    }

    public static boolean mayBeAutoCasted (ExpressionType from, ExpressionType to) {
	List<ExpressionType> l = ALLOWED_UPCASTS.get (from);
	return l != null && l.contains (to);
    }

    /**
     * @param className fully qualified class name "foo.bar.Baz"
     */
    private ExpressionType (String className, boolean isPrimitive, int type) {
	if (className == null)
	    throw new NullPointerException ("className may not be null");
	this.className = className;
	slashName = isPrimitive ? className : className.replace ('.', '/');
	this.isPrimitive = isPrimitive;
	this.type = type;
    }

    /**
     * @param className fully qualified class name "foo.bar.Baz"
     */
    public ExpressionType (String className) {
	this (className, false, Type.OBJECT);
    }

    public static ExpressionType get (Type t) {
	if (t.getSort () == Type.OBJECT)
	    return new ExpressionType (t.getInternalName ().replace ('/', '.'));
	for (ExpressionType et : primitives)
	    if (et.type == t.getSort ())
		return et;
	throw new IllegalArgumentException ("Unknown type: " + t);
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

    public String getSlashName () {
	return slashName;
    }

    public String getDescriptor () {
	if (isPrimitive)
	    return slashName;
	return "L" + slashName + ";";
    }

    public String getPrimitiveName () {
	if (isPrimitiveType ())
	    return className;
	throw new IllegalStateException ("Not a primitive type");
    }

    public boolean match (Type t) {
	if (type == Type.OBJECT && t.getSort () == Type.OBJECT &&
	    t.getInternalName ().equals (slashName)) {
	    return true;
	} else if (type == t.getSort () || (isPrimitive && mayBeAutoCasted (this, t))) {
	    return true;
	}
	return false;
    }

    private static boolean mayBeAutoCasted (ExpressionType from, Type t) {
	List<ExpressionType> l = ALLOWED_UPCASTS.get (from);
	if (l == null)
	    return false;
	for (ExpressionType et : l)
	    if (et.type == t.getSort ())
		return true;
	return false;
    }
}
