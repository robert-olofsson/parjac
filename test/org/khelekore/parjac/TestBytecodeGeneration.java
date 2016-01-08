package org.khelekore.parjac;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
	Object ret = compileAndRun ("public class Foo { public static void foo () { return; }}");
	assert ret == null : "Got something back from void method: " + ret;
    }

    @Test
    public void testIntReturnValue () throws IOException, ReflectiveOperationException {
	int[] values = {0, 1, 5, 6, 9, 17, 127, 128, 16535, 16536, 32452345,
			-1, -7, -127, -128, -129, -16536, -123456789};
	for (int v : values) {
	    String src = "public class Foo { public static int foo () { return " + v + "; }}";
	    Object ret = compileAndRun (src);
	    assert ret.equals (v) : "Got wrong thing back: " + ret + ", expected: " + v;
	}
    }

    @Test
    public void testDoubleReturnValue () throws IOException, ReflectiveOperationException {
	double[] values = {0.0, 1.0, 3.14, 99.99, 2134e67,
			   -1.0 -3.14, -99.99, -2134e67};
	for (double v : values) {
	    String src = "public class Foo { public static double foo () { return " + v + "; }}";
	    Object ret = compileAndRun (src);
	    assert ret.equals (v) : "Got wrong thing back: " + ret + ", expected: " + v;
	}
    }

    @Test
    public void testFloatReturnValue () throws IOException, ReflectiveOperationException {
	float[] values = {0.0f, 1.0f, 3.14f, 99.99f, 2134e12f,
			   -1.0f -3.14f, -99.99f, -2134e12f};
	for (float v : values) {
	    String src = "public class Foo { public static float foo () { return " + v + "f; }}";
	    Object ret = compileAndRun (src);
	    assert ret.equals (v) : "Got wrong thing back: " + ret +
		", ret.class: " + ret.getClass ().getName () +", expected: " + v;
	}
    }

    @Test
    public void testBooleanReturnValue () throws IOException, ReflectiveOperationException {
	boolean[] values = {true, false};
	for (boolean b : values) {
	    String src = "public class Foo { public static boolean foo () { return " + b + "; }}";
	    Object ret = compileAndRun (src);
	    assert ret.equals (b) : "Got wrong thing back: " + ret +
		", ret.class: " + ret.getClass ().getName () +", expected: " + b;
	}
    }

    @Test
    public void testNullReturn () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRun ("public class Foo { public static String foo () { return null; }}");
	assert ret == null : "Got something back from null method: " + ret;
	ret = compileAndRun ("public class Foo { public static Foo foo () { return null; }}");
	assert ret == null : "Got something back from null method: " + ret;
	ret = compileAndRun ("import java.util.Map;\n" +
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
	    Object ret = compileAndRun (src);
	    assert ret.equals (s) : "Got wrong thing back: " + ret +
		", ret.class: " + ret.getClass ().getName () +", expected: " + s;
	}
    }

    @Test
    public void testSimpleExternalMethodCall () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRun ("public class Foo { public static int foo () {" +
				    " return Integer.parseInt (\"3\"); }}");
	assert ret instanceof Integer : "Got wrong type back: " + ret;
	assert 3 == (Integer)ret : "Got wrong result, expected 3, got: " + ret;

	ret = compileAndRun ("public class Foo { public static int foo () {" +
			     " return \"Hello World!\".length (); }}");
	assert ret instanceof Integer : "Got wrong type back: " + ret;
	assert 12 == (Integer)ret : "Got wrong result, expected 12, got: " + ret;
    }

    @Test
    public void testSimpleInternalMethodCall () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRun ("public class Foo { " +
				    "static int a () { return 3; }" +
				    "public static int foo () { return a (); }}");
	assert ret instanceof Integer : "Got wrong type back: " + ret;
	assert 3 == (Integer)ret : "Got wrong result, expected 3, got: " + ret;
    }

    @Test
    public void testInnerClassInternalMethodCall () throws IOException, ReflectiveOperationException {
	Object ret = compileAndRun ("public class Foo { " +
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
				Object argument, int expectedResult) throws ReflectiveOperationException {
	Method m = c.getMethod (methodName, argumentType);
	Object ret = m.invoke (null, argument);
	assert ret instanceof Integer : "Got wrong type back: " + ret;
	assert expectedResult == (Integer)ret : "Got wrong result, expected " + expectedResult + ", got: " + ret;
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

    private Object compileAndRun (String s) throws ReflectiveOperationException {
	Class<?> c = getClass (s, "Foo");
	Method m = c.getMethod ("foo");
	return m.invoke (null);
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
