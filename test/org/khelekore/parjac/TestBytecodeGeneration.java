package org.khelekore.parjac;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    private Object compileAndRun (String s) throws ReflectiveOperationException {
	Class<?> c = getClass (s);
	Method m = c.getMethod ("foo");
	return m.invoke (null);
    }

    private Class<?> getClass (String s) throws ReflectiveOperationException {
	CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	SourceProvider sp = new StringSourceProvider (Paths.get ("Foo.java"), s);
	MemoryBytecodeWriter bw = new MemoryBytecodeWriter ();
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
	final byte[] b = bw.getBytecode (Paths.get ("Foo.class"));
	ClassLoader cl = new ClassLoader () {
	    protected Class<?> findClass (String name) throws ClassNotFoundException {
		return defineClass (name, b, 0, b.length);
	    }
	};
	return cl.loadClass ("Foo");
    }
}
