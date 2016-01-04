package org.khelekore.parjac.tree;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.Type;

public class ExpressionType {
    private final String className;
    private final String slashName;
    private final boolean isPrimitive;
    private final int dims;
    private final int type;

    public static final ExpressionType BYTE = new ExpressionType ("B", true, Type.BYTE, 0);
    public static final ExpressionType SHORT = new ExpressionType ("S", true, Type.SHORT, 0);
    public static final ExpressionType CHAR = new ExpressionType ("C", true, Type.CHAR, 0);
    public static final ExpressionType INT = new ExpressionType ("I", true, Type.INT, 0);
    public static final ExpressionType LONG = new ExpressionType ("J", true, Type.LONG, 0);
    public static final ExpressionType FLOAT = new ExpressionType ("F", true, Type.FLOAT, 0);
    public static final ExpressionType DOUBLE = new ExpressionType ("D", true, Type.DOUBLE, 0);
    public static final ExpressionType BOOLEAN = new ExpressionType ("Z", true, Type.BOOLEAN, 0);

    public static final ExpressionType VOID = new ExpressionType ("V", true, Type.VOID, 0);
    public static final ExpressionType NULL = new ExpressionType ("null", false, -1, 0);

    public static final ExpressionType STRING = new ExpressionType ("java.lang.String", false, Type.OBJECT, 0);
    public static final ExpressionType OBJECT = new ExpressionType ("java.lang.Object", false, Type.OBJECT, 0);

    private static final ExpressionType[] primitives =
    { BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE, BOOLEAN, VOID };

    private final static Map<ExpressionType, List<ExpressionType>> ALLOWED_UPCASTS = new HashMap<> ();
    private final static Set<ExpressionType> INTEGRAL_TYPES = new HashSet<> ();
    private final static Set<ExpressionType> NUMERIC_TYPES = new HashSet<> ();
    static {
	ALLOWED_UPCASTS.put (BYTE, Arrays.asList (SHORT, INT, LONG));
	ALLOWED_UPCASTS.put (SHORT, Arrays.asList (INT, LONG));
	ALLOWED_UPCASTS.put (CHAR, Arrays.asList (INT, LONG));
	ALLOWED_UPCASTS.put (INT, Arrays.asList (LONG));
	ALLOWED_UPCASTS.put (FLOAT, Arrays.asList (DOUBLE));

	INTEGRAL_TYPES.add (BYTE);
	INTEGRAL_TYPES.add (SHORT);
	INTEGRAL_TYPES.add (CHAR);
	INTEGRAL_TYPES.add (INT);
	INTEGRAL_TYPES.add (LONG);

	NUMERIC_TYPES.addAll (INTEGRAL_TYPES);
	NUMERIC_TYPES.add (FLOAT);
	NUMERIC_TYPES.add (DOUBLE);
    }

    public static boolean mayBeAutoCasted (ExpressionType from, ExpressionType to) {
	List<ExpressionType> l = ALLOWED_UPCASTS.get (from);
	return l != null && l.contains (to);
    }

    /**
     * @param className fully qualified class name "foo.bar.Baz"
     */
    private ExpressionType (String className, boolean isPrimitive, int type, int dims) {
	if (className == null)
	    throw new NullPointerException ("className may not be null");
	this.isPrimitive = isPrimitive;
	this.type = type;
	this.dims = dims;
	String cn = className;
	String sn = className;
	if (!isPrimitive)
	    sn = className.replace ('.', '/');
	this.className = cn;
	this.slashName = sn;
    }

    /**
     * @param className fully qualified class name "foo.bar.Baz"
     */
    public ExpressionType (String className) {
	this (className, false, Type.OBJECT, 0);
    }

    public static ExpressionType array (ExpressionType base, int dims) {
	return new ExpressionType (base.className, false, Type.ARRAY, dims);
    }

    public static ExpressionType get (Type t) {
	if (t.getSort () == Type.ARRAY) {
	    ExpressionType base = get (t.getElementType ());
	    return array (base, t.getDimensions ());
	}
	if (t.getSort () == Type.OBJECT)
	    return new ExpressionType (t.getInternalName ().replace ('/', '.'));
	for (ExpressionType et : primitives)
	    if (et.type == t.getSort ())
		return et;
	throw new IllegalArgumentException ("Unknown type: " + t);
    }

    public static ExpressionType bigger (ExpressionType e1, ExpressionType e2) {
	if (mayBeAutoCasted (e1, e2))
	    return e2;
	if (e1 == NULL)
	    return e2;
	return e1;
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
	    e.dims == dims &&
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

    public boolean isArray () {
	return dims > 0;
    }

    public int getDimensions () {
	return dims;
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
	String res = slashName;
	if (!isPrimitive)
	    res = "L" + slashName + ";";
	if (isArray ())
	    res = repeat ("[", dims) + res;
	return res;
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

    private static String repeat (String s, int times) {
	StringBuilder sb = new StringBuilder ();
	for (int i = 0; i < times; i++)
	    sb.append (s);
	return sb.toString ();
    }

    public boolean isIntegralType () {
	return INTEGRAL_TYPES.contains (this);
    }

    public boolean isNumericType () {
	return NUMERIC_TYPES.contains (this);
    }

    public boolean isBooleanType () {
	return this == BOOLEAN;
    }
}
