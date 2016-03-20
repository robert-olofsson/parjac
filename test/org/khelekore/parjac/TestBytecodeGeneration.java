package org.khelekore.parjac;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.parser.TestParseHelper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestBytecodeGeneration {
    private Grammar g;

    @BeforeClass
    public void beforeClass () throws IOException {
	g = TestParseHelper.getJavaGrammarFromFile ("CompilationUnit", false);
    }

    @Test
    public void testReturn () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRunStatic ("public class Foo { public static void foo () { return; }}");
	assert ret == null : "Got something back from void method: " + ret;
    }

    @Test
    public void testIntReturnValue () throws IOException, ReflectiveOperationException {
	int[] values = {0, 1, 5, 6, 9, 17, 127, 128, 16535, 16536, 32452345,
			-1, -7, -127, -128, -129, -16536, -123456789};
	for (int v : values) {
	    String src = "public class Foo { public static int foo () { return " + v + "; }}";
	    Object ret = compileAndRunStatic (src);
	    assert ret.equals (v) : "Got wrong thing back: " + ret + ", expected: " + v;
	}
    }

    @Test
    public void testDoubleReturnValue () throws IOException, ReflectiveOperationException {
	double[] values = {0.0, 1.0, 3.14, 99.99, 2134e67,
			   -1.0 -3.14, -99.99, -2134e67};
	for (double v : values) {
	    String src = "public class Foo { public static double foo () { return " + v + "; }}";
	    Object ret = compileAndRunStatic (src);
	    assert ret.equals (v) : "Got wrong thing back: " + ret + ", expected: " + v;
	}
    }

    @Test
    public void testFloatReturnValue () throws IOException, ReflectiveOperationException {
	float[] values = {0.0f, 1.0f, 3.14f, 99.99f, 2134e12f,
			   -1.0f -3.14f, -99.99f, -2134e12f};
	for (float v : values) {
	    String src = "public class Foo { public static float foo () { return " + v + "f; }}";
	    Object ret = compileAndRunStatic (src);
	    assert ret.equals (v) : "Got wrong thing back: " + ret +
		", ret.class: " + ret.getClass ().getName () +", expected: " + v;
	}
    }

    @Test
    public void testBooleanReturnValue () throws IOException, ReflectiveOperationException {
	boolean[] values = {true, false};
	for (boolean b : values) {
	    String src = "public class Foo { public static boolean foo () { return " + b + "; }}";
	    Object ret = compileAndRunStatic (src);
	    assert ret.equals (b) : "Got wrong thing back: " + ret +
		", ret.class: " + ret.getClass ().getName () +", expected: " + b;
	}
    }

    @Test
    public void testNullReturn () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRunStatic ("public class Foo { public static String foo () { return null; }}");
	assert ret == null : "Got something back from null method: " + ret;
	ret = compileAndRunStatic ("public class Foo { public static Foo foo () { return null; }}");
	assert ret == null : "Got something back from null method: " + ret;
	ret = compileAndRunStatic ("import java.util.Map;\n" +
			     "public class Foo {\n" +
			     "    public static Map.Entry foo () { return null; }\n"+
			     "}");
	assert ret == null : "Got something back from null method: " + ret;
    }

    @Test
    public void testStringReturnValue () throws IOException, ReflectiveOperationException {
	String[] values = {"", "adsf", "asdfasdfasdfasdfasdadf"};
	for (String s : values) {
	    String src = "public class Foo { public static String foo () { return \"" + s + "\"; }}";
	    Object ret = compileAndRunStatic (src);
	    assert ret.equals (s) : "Got wrong thing back: " + ret +
		", ret.class: " + ret.getClass ().getName () +", expected: " + s;
	}
    }

    @Test
    public void testSimpleExternalMethodCall () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRunStatic ("public class Foo { public static int foo () {" +
				    " return Integer.parseInt (\"3\"); }}");
	assert ret instanceof Integer : "Got wrong type back: " + ret;
	assert 3 == (Integer)ret : "Got wrong result, expected 3, got: " + ret;

	ret = compileAndRunStatic ("public class Foo { public static int foo () {" +
			     " return \"Hello World!\".length (); }}");
	assert ret instanceof Integer : "Got wrong type back: " + ret;
	assert 12 == (Integer)ret : "Got wrong result, expected 12, got: " + ret;
    }

    @Test
    public void testSimpleInternalMethodCall () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRunStatic ("public class Foo { " +
					  "static int a () { return 3; }" +
					  "public static int foo () { return a (); }}");
	assert ret instanceof Integer : "Got wrong type back: " + ret;
	assert 3 == (Integer)ret : "Got wrong result, expected 3, got: " + ret;
    }

    @Test
    public void testInternalInstanceMethod () throws IOException, ReflectiveOperationException {
	String s = "public class Foo { public int foo () { bar(); return 3; } private void bar() {}}";
	MethodTypeAndArgs mta = new MethodTypeAndArgs (new Class<?>[0], new Object[0]);
	Object o = compileAndRunInstanceMethod (s, mta);
	checkResultObject (o, Integer.class, 3);
    }

    @Test
    public void testInnerClassInternalMethodCall () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRunStatic ("public class Foo { " +
				    "static class A { static int a () { return 3; }}" +
				    "public static int foo () { return A.a (); }}");
	assert ret instanceof Integer : "Got wrong type back: " + ret;
	assert 3 == (Integer)ret : "Got wrong result, expected 3, got: " + ret;
    }

    @Test
    public void testSimpleIf () throws IOException, ReflectiveOperationException {
	String s = "public class IF { public static int i (boolean b) { if (b) return 3; return 4; }}";
	Class<?> c = getClass (s, "IF");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.TRUE, 3);
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.FALSE, 4);
    }

    @Test
    public void testNegatedIf () throws IOException, ReflectiveOperationException {
	String s = "public class IF { public static int i (boolean b) { if (!b) return 3; return 4; }}";
	Class<?> c = getClass (s, "IF");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.TRUE, 4);
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.FALSE, 3);
    }

    @Test
    public void testBooleanComparisson () throws IOException, ReflectiveOperationException {
	// oddly enough javac actually compiles differently for a == false and false == a
	String s = "public class IF { public static int i (boolean b) { if (b == false) return 3; return 4; }}";
	Class<?> c = getClass (s, "IF");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.TRUE, 4);
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.FALSE, 3);

	s = "public class IF { public static int i (boolean b) { if (false == b) return 3; return 4; }}";
	c = getClass (s, "IF");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.TRUE, 4);
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.FALSE, 3);
    }

    @Test
    public void testIntComparisson () throws IOException, ReflectiveOperationException {
	String s = "public class IF { public static int i (int a) { if (a == 3) return 3; return 4; }}";
	Class<?> c = getClass (s, "IF");
	checkIfReturn (c, "i", Integer.TYPE, 3, 3);
	checkIfReturn (c, "i", Integer.TYPE, 17, 4);
    }

    @Test
    public void testObjectComparisson () throws IOException, ReflectiveOperationException {
	String s = "public class IF { public static int i (Object o) { if (o == null) return 3; return 4; }}";
	Class<?> c = getClass (s, "IF");
	checkIfReturn (c, "i", Object.class, null, 3);
	checkIfReturn (c, "i", Object.class, new Object (), 4);
    }

    private void checkIfReturn (Class<?> c, String methodName, Class<?> argumentType,
				Object argument, Object expectedResult) throws ReflectiveOperationException {
	Method m = c.getMethod (methodName, argumentType);
	Object ret = m.invoke (null, argument);
	assert ret.getClass ().equals (expectedResult.getClass ()) : "Got wrong type back: " + ret;
	assert expectedResult.equals (ret) :
	"Got wrong result, expected " + expectedResult + ", got: " + ret;
    }

    @Test
    public void testSimpleWhile () throws IOException, ReflectiveOperationException {
	String s = "public class WHILE { public static void f (Runnable r) {" +
	    "    int i = 0; while (i < 7) { r.run (); i++; }}}";
	checkRunnableLoop (s, "WHILE", "f", 7);
    }

    @Test
    public void testSimpleDo () throws IOException, ReflectiveOperationException {
	String s = "public class DO { public static void f (Runnable r) {" +
	    "    int i = 0; do { r.run (); i++; } while (i < 8); }}";
	checkRunnableLoop (s, "DO", "f", 8);
    }

    @Test
    public void testSimpleFor () throws IOException, ReflectiveOperationException {
	String s = "public class FOR { public static void f (Runnable r) {" +
	    "    for (int i = 0; i < 10; i++) r.run (); }}";
	checkRunnableLoop (s, "FOR", "f", 10);
	s = "public class FOR { public static void f (Runnable r) {" +
	    "    for (int i = 10; i > 0; i--) r.run (); }}";
	checkRunnableLoop (s, "FOR", "f", 10);
	s = "public class FOR { public static void f (Runnable r) {" +
	    "    for (int i = 10, j = 0; i > j; i--) r.run (); }}";
	checkRunnableLoop (s, "FOR", "f", 10);
    }

    private void checkRunnableLoop (String s, String clzName, String methodName, int expectedLoops)
	throws IOException, ReflectiveOperationException {
	Class<?> c = getClass (s, clzName);
	AtomicInteger counter = new AtomicInteger (0);
	Runnable r = () -> counter.incrementAndGet ();
	Method m = c.getMethod (methodName, Runnable.class);
	m.invoke (null, r);
	assert counter.get () == expectedLoops :
	"Got wrong result: " + counter.get () + ", expected: " + expectedLoops;
    }

    @Test
    public void testAutoCast () throws IOException, ReflectiveOperationException {
	checkResult ("public class Foo { public static int foo () { byte l = 3; return l; }}", Integer.class, 3);
	checkResult ("public class Foo { public static int foo () { char l = 3; return l; }}", Integer.class, 3);
	checkResult ("public class Foo { public static int foo () { short l = 3; return l; }}", Integer.class, 3);

	checkResult ("public class Foo { public static long foo () { int l = 3; return l; }}", Long.class, 3);
	checkResult ("public class Foo { public static long foo () { short l = 3; return l; }}", Long.class, 3);
	checkResult ("public class Foo { public static long foo () { char l = 3; return l; }}", Long.class, 3);
	checkResult ("public class Foo { public static long foo () { byte l = 3; return l; }}", Long.class, 3);

	checkResult ("public class Foo { public static double foo () { int l = 3; return l; }}", Double.class, 3);
	checkResult ("public class Foo { public static double foo () { short l = 3; return l; }}", Double.class, 3);
	checkResult ("public class Foo { public static double foo () { char l = 3; return l; }}", Double.class, 3);
	checkResult ("public class Foo { public static double foo () { byte l = 3; return l; }}", Double.class, 3);
    }

    @Test
    public void testNonIntTypes () throws IOException, ReflectiveOperationException {
	checkResult ("public class Foo { public static long foo () { long l = 3L; return l; }}",
		     Long.class, 3);
	checkResult ("public class Foo { public static float foo () { float f = 3.0f; return f; }}",
		     Float.class, 3);
	checkResult ("public class Foo { public static double foo () { double d = 3.0; return d; }}",
		     Double.class, 3);
    }

    @Test
    public void testAutoConvertLiterals () throws IOException, ReflectiveOperationException {
	checkResult ("public class Foo { public static long foo () { long l = 3; return l; }}",
		     Long.class, 3);
	checkResult ("public class Foo { public static float foo () { float l = 3; return l; }}",
		     Float.class, 3);
	checkResult ("public class Foo { public static double foo () { double l = 3; return l; }}",
		     Double.class, 3);

	checkResult ("public class Foo { public static double foo () { double l = 3f; return l; }}",
		     Double.class, 3);
    }

    @Test
    public void testTwoPartExpressions () throws IOException, ReflectiveOperationException {
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 9; int j = 6; int k; k = i - j; return k; }}", Integer.class, 3);

	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 9; int j = 6; long k; k = i - j; return k; }}", Long.class, 3);
    }

    @Test
    public void testAssignWithOpInt () throws IOException, ReflectiveOperationException {
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 3; i *= 5; return i; }}", Integer.class, 15);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 13; i /= 5; return i; }}", Integer.class, 2);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 13; i %= 5; return i; }}", Integer.class, 3);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 13; i += 5; return i; }}", Integer.class, 18);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 13; i -= 5; return i; }}", Integer.class, 8);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 3; i <<= 2 ; return i; }}", Integer.class, 12);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 12; i >>= 2 ; return i; }}", Integer.class, 3);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 0xffffffff; i >>>= 8 ; return i; }}", Integer.class, 0xffffff);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 0xf; i &= 0x3 ; return i; }}", Integer.class, 3);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 0xa; i ^= 0x3 ; return i; }}", Integer.class, 9);
	checkResult ("public class Foo { public static int foo () { " +
		     "int i = 0xa; i |= 0x3 ; return i; }}", Integer.class, 11);
    }

    @Test
    public void testAssignWithOpLong () throws IOException, ReflectiveOperationException {
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 3; i *= 5; return i; }}", Long.class, 15);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 13; i /= 5; return i; }}", Long.class, 2);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 13; i %= 5; return i; }}", Long.class, 3);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 13; i += 5; return i; }}", Long.class, 18);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 13; i -= 5; return i; }}", Long.class, 8);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 3; i <<= 2 ; return i; }}", Long.class, 12);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 12; i >>= 2 ; return i; }}", Long.class, 3);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 0xffffffff_ffffffffL; i >>>= 62 ; return i; }}", Long.class, 3);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 0xf; i &= 0x3 ; return i; }}", Long.class, 3);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 0xa; i ^= 0x3 ; return i; }}", Long.class, 9);
	checkResult ("public class Foo { public static long foo () { " +
		     "long i = 0xa; i |= 0x3 ; return i; }}", Long.class, 11);
    }

    @Test
    public void testAssignWithOpFloat () throws IOException, ReflectiveOperationException {
	checkResult ("public class Foo { public static float foo () { " +
		     "float i = 3; i *= 5; return i; }}", Float.class, 15);
	checkResult ("public class Foo { public static float foo () { " +
		     "float i = 13; i /= 5; return i; }}", Float.class, 2);
	checkResult ("public class Foo { public static float foo () { " +
		     "float i = 13; i %= 5; return i; }}", Float.class, 3);
	checkResult ("public class Foo { public static float foo () { " +
		     "float i = 13; i += 5; return i; }}", Float.class, 18);
	checkResult ("public class Foo { public static float foo () { " +
		     "float i = 13; i -= 5; return i; }}", Float.class, 8);
    }

    @Test
    public void testAssignWithOpDouble () throws IOException, ReflectiveOperationException {
	checkResult ("public class Foo { public static double foo () { " +
		     "double i = 3; i *= 5; return i; }}", Double.class, 15);
	checkResult ("public class Foo { public static double foo () { " +
		     "double i = 13; i /= 5; return i; }}", Double.class, 2);
	checkResult ("public class Foo { public static double foo () { " +
		     "double i = 13; i %= 5; return i; }}", Double.class, 3);
	checkResult ("public class Foo { public static double foo () { " +
		     "double i = 13; i += 5; return i; }}", Double.class, 18);
	checkResult ("public class Foo { public static double foo () { " +
		     "double i = 13; i -= 5; return i; }}", Double.class, 8);
    }

    @Test
    public void testInstanceMethod () throws IOException, ReflectiveOperationException {
	MethodTypeAndArgs mta = new MethodTypeAndArgs (new Class<?>[]{Integer.TYPE}, new Object[]{5});
	Object ret = compileAndRunInstanceMethod ("public class Foo { public Foo () {}" +
						  "public int foo (int j) { int i = j + 3; return i; }}", mta);
	checkResultObject (ret, Integer.class, 8);
    }

    @Test
    public void testConstructor () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRunStatic ("public class Foo { public Foo () {}" +
					  "public static Foo foo () { return new Foo (); }}");
	assert ret != null : "Got null back";
	String clz = ret.getClass ().getName ();
	assert clz.equals ("Foo") : "Got wrong class back: " + clz;

	ret = compileAndRunStatic ("public class Foo { public Foo (int i) {}" +
				   "public static Foo foo () { return new Foo (4711); }}");
	assert ret != null : "Got null back";
	clz = ret.getClass ().getName ();
	assert clz.equals ("Foo") : "Got wrong class back: " + clz;
    }

    @Test
    public void testStaticFieldInitializer () throws IOException, ReflectiveOperationException {
	Class<?> c = getClass ("public class C { public static Object a = new C(); }", "C");
	assert c != null : "Failed to compile class";
	Field f = c.getDeclaredField ("a");
	assert f != null : "Failed to find field";
	Object o = f.get (null);
	assert o != null : "Field should be non-null";
    }

    @Test
    public void testStaticFieldPrimitiveInitializer () throws IOException, ReflectiveOperationException {
	Class<?> c = getClass ("public class C { public static int a = 3; }", "C");
	assert c != null : "Failed to compile class";
	Field f = c.getDeclaredField ("a");
	assert f != null : "Failed to find field";
	Object o = f.get (null);
	assert ((Number)o).intValue () == 3 : "Wrong value: expected: 3, got: " + o;
    }

    @Test
    public void testConstructorCode () throws IOException, ReflectiveOperationException {
	Class<?> c = getClass ("public class C { public int a; public C () { a = 30; }}", "C");
	assert c != null : "Failed to compile class";
	Object instance = c.newInstance ();
	checkFieldValue (c, instance, "a", 30);
    }

    @Test
    public void testThis () throws IOException, ReflectiveOperationException {
	Class<?> c = getClass ("public class A { public int i; public A (int i) { this.i = i; }}", "A");
	assert c != null : "Failed to compile class";
	Constructor<?> cc = c.getConstructor (Integer.TYPE);
	Object instance = cc.newInstance (17);
	checkFieldValue (c, instance, "i", 17);
    }

    @Test
    public void testThisOther () throws IOException, ReflectiveOperationException {
	Class<?> c = getClass ("public class A { public int i; " +
			       "    public A (int a) { this.i = a; }" +
			       "    public A (A other) { this.i = other.i; }}", "A");
	assert c != null : "Failed to compile class";
	Constructor<?> cc1 = c.getConstructor (Integer.TYPE);
	Object instance1 = cc1.newInstance (19);
	Constructor<?> cc2 = c.getConstructor (c);
	Object instance2 = cc2.newInstance (instance1);
	checkFieldValue (c, instance2, "i", 19);
    }

    @Test
    public void testStringConcat () throws IOException, ReflectiveOperationException {
	Object o = compileAndRunStatic ("public class Foo {" +
					"    public static String foo () {" +
					"        Foo a = new Foo ();" +
					"        return \"a: \" + a; }}");
	assert o != null : "Got null back";
	assert o instanceof String : "Got wrong type back: " + o.getClass ().getName ();
    }

    @Test
    public void testTernery () throws IOException, ReflectiveOperationException {
	String s = "public class IF { public static int i (boolean b) { return b ? 3 : 4; }}";
	Class<?> c = getClass (s, "IF");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.TRUE, 3);
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.FALSE, 4);

	s = "public class IF { public static long i (boolean b) { byte s = 3; return b ? s : 12L; }}";
	c = getClass (s, "IF");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.TRUE, 3L);
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.FALSE, 12L);

	s = "public class IF { public static long i (boolean b) { byte s = 3; return b ? 12L : s; }}";
	c = getClass (s, "IF");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.TRUE, 12L);
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.FALSE, 3L);

	s = "public class IF { public static Object i (boolean b) { return b ? \"\" : Boolean.TRUE; }}";
	c = getClass (s, "IF");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.TRUE, "");
	checkIfReturn (c, "i", Boolean.TYPE, Boolean.FALSE, Boolean.TRUE);
    }

    @Test
    public void testReturnField () throws IOException, ReflectiveOperationException {
	String s = "public class Foo { int a; public int foo () { a += 5; return a; }}";
	MethodTypeAndArgs mta = new MethodTypeAndArgs (new Class<?>[0], new Object[0]);
	Object o = compileAndRunInstanceMethod (s, mta);
	checkResultObject (o, Integer.class, 5);
    }

    @Test
    public void testSynchronizedBlock () throws IOException, ReflectiveOperationException {
	String s = "public class Foo { public int foo () throws Exception { synchronized (this) { wait (1); } return 3; }}";
	MethodTypeAndArgs mta = new MethodTypeAndArgs (new Class<?>[0], new Object[0]);
	Object o = compileAndRunInstanceMethod (s, mta);
	checkResultObject (o, Integer.class, 3);
    }

    @Test
    public void testSynchronizedMethod () throws IOException, ReflectiveOperationException {
	String s = "public class Foo { public synchronized int foo () throws Exception { wait (1); return 3; }}";
	MethodTypeAndArgs mta = new MethodTypeAndArgs (new Class<?>[0], new Object[0]);
	Object o = compileAndRunInstanceMethod (s, mta);
	checkResultObject (o, Integer.class, 3);
    }

    @Test
    public void testExceptionThrown () throws IOException, ReflectiveOperationException {
	String s = "import java.util.concurrent.Callable;" +
	    "public class Foo { public static int f (Callable c) {" +
	    "    int res = 3; try { c.call (); } catch (Exception e) { res = 4; } return res; }}";
	Class<?> c = getClass (s, "Foo");
	Callable<Void> c1 = () -> {return null;};
	Method m = c.getMethod ("f", Callable.class);
	Object o = m.invoke (null, c1);
	checkResultObject (o, Integer.class, 3);
	Callable<Void> c2 = () -> {throw new Exception ("bummer");};
	o = m.invoke (null, c2);
	checkResultObject (o, Integer.class, 4);
    }

    private void checkResult (String s, Class<?> retType, int expected)
	throws IOException, ReflectiveOperationException {
	Object ret = compileAndRunStatic (s);
	checkResultObject (ret, retType, expected);
    }

    private void checkResultObject (Object ret, Class<?> retType, int expected) {
	assert ret.getClass () == retType : "Got wrong type back: " + ret.getClass ().getName ();
	assert ((Number)ret).intValue () == expected : "Got wrong result, expected " + expected + ", got: " + ret;
    }

    private void checkFieldValue (Class<?> c, Object instance, String field, int expected)
	throws ReflectiveOperationException {
	Field f = c.getDeclaredField (field);
	Object o = f.get (instance);
	assert ((Number)o).intValue () == expected : "Wrong value: expected: " + expected + ", got: " + o;
    }

    private Object compileAndRunStatic (String s) throws ReflectiveOperationException {
	Class<?> c = getClass (s, "Foo");
	String methodName = "foo";
	Method m = c.getMethod (methodName);
	assert m != null : "Found no method named: " + methodName;
	return m.invoke (null);
    }

    private Object compileAndRunInstanceMethod (String s, MethodTypeAndArgs mta)
	throws ReflectiveOperationException {
	Class<?> c = getClass (s, "Foo");
	Object instance = c.newInstance ();
	Method m = c.getMethod ("foo", mta.paramTypes);
	return m.invoke (instance, mta.args);
    }

    public static class MethodTypeAndArgs {
	public Class<?>[] paramTypes;
	public Object[] args;

	public MethodTypeAndArgs () {
	    paramTypes = new Class<?>[0];
	    args = new Object[0];
	}

	public MethodTypeAndArgs (Class<?>[] paramTypes, Object[] args) {
	    this.paramTypes = paramTypes;
	    this.args = args;
	}
    }

    private Class<?> getClass (String s, String className) throws ReflectiveOperationException {
	CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	SourceProvider sp = new StringSourceProvider (Paths.get (className + ".java"), s);
	final MemoryBytecodeWriter bw = new MemoryBytecodeWriter ();
	List<Path> classPathEntries = Collections.emptyList ();
	CompilationArguments settings =
	    new CompilationArguments (sp, bw, classPathEntries, false, false);
	Compiler c = new Compiler (diagnostics, g, settings);
	c.compile ();
	if (diagnostics.hasError ()) {
	    diagnostics.getDiagnostics ().
		forEach (d -> System.err.println (d.getMessage (Locale.getDefault ())));
	    assert false : "Compilation generated errors";
	}
	ClassLoader cl = new ClassLoader () {
	    protected Class<?> findClass (String name) throws ClassNotFoundException {
		byte[] b = bw.getBytecode (Paths.get (name + ".class"));
		return defineClass (name, b, 0, b.length);
	    }
	};
	return cl.loadClass (className);
    }
}
