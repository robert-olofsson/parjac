package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestMethodDeclaration {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParseHelper.getJavaGrammarFromFile ("MethodDeclaration", false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSimpleMethods () {
	testSuccessfulParse ("void foo () { }");
	testSuccessfulParse ("int bar (int a) { }");
	testSuccessfulParse ("@Override public String toString () { }");
	testSuccessfulParse ("@Override final strictfp synchronized protected Foo " +
			     " foo (Foo foo, Bar bar, Baz baz) { }");
    }

    @Test
    public void testArrayMethods () {
	testSuccessfulParse ("int[] foo () { }");
	testSuccessfulParse ("int[][] foo () { }");
	testSuccessfulParse ("int[][][] foo () { }");
	testSuccessfulParse ("void foo (int[] a) { }");
	testSuccessfulParse ("void foo (int[][] a) { }");
	testSuccessfulParse ("void foo (int[][][] a) { }");
	testSuccessfulParse ("void foo (int a[]) { }");
	testSuccessfulParse ("void foo (int a[][]) { }");
	testSuccessfulParse ("void foo (int[] a, Foo[] b) { }");
    }

    @Test
    public void testNonBodyMethods () {
	testSuccessfulParse ("abstract int foo ();");
	testSuccessfulParse ("final native double bar ();");
    }

    @Test
    public void testTypedMethod () {
	testSuccessfulParse ("<T> void foo () { }");
	testSuccessfulParse ("<T> void foo (T t) { }");
	testSuccessfulParse ("<T> void foo (float t) { }");
	testSuccessfulParse ("<K, V> void foo (K k, V v) { }");
    }

    @Test
    public void testEllipsis () {
	testSuccessfulParse ("void foo (int... is) {}");
	testSuccessfulParse ("void foo (Foo... foos) {}");
	testSuccessfulParse ("void foo (@NotNull Foo... foos) {}");
	testSuccessfulParse ("void foo (Foo foo, Bar... is) {}");
	testSuccessfulParse ("void foo (Foo foo, int... is) {}");
    }

    @Test
    public void testBadEllipsis () {
	testFailedParse ("void foo (int... is, Foo foo) {}");
	testFailedParse ("void foo (Foo foo, int... is, Foo foo) {}");
    }

    @Test
    public void testAnnotated () {
	testSuccessfulParse ("void foo (@NotNull final Foo foo) {}");
	testSuccessfulParse ("void foo (@NotNull Foo foo) {}");
    }

    @Test
    public void testReceiverParameter () {
	testSuccessfulParse ("void foo (Foo this) {}");
	testSuccessfulParse ("void foo (Foo Foo.this) {}");
	testSuccessfulParse ("void foo (@NotNull Foo this) {}");
	testSuccessfulParse ("void foo (@NotNull @ReadOnly Foo this) {}");
	testSuccessfulParse ("void foo (Foo this, Bar bar) {}");
	testFailedParse ("void foo (Foo foo, Bar this) {}");
	testSuccessfulParse ("void innerMethod(@A Outer.@B Middle.@C Inner this) {}");
    }

    @Test
    public void testThrows () {
	testSuccessfulParse ("void foo () throws A {}");
	testSuccessfulParse ("void foo () throws A, B, C {}");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.earleyParse (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }

    private void testFailedParse (String s) {
	try {
	    TestParseHelper.earleyParse (g, s, diagnostics);
	    assert diagnostics.hasError () : "Failed to detect errors";
	} finally {
	    diagnostics.clear ();
	}
    }
}