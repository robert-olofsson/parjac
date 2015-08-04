package org.khelekore.parjac.semantics;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.tree.SyntaxTree;
import org.testng.annotations.Test;

public class TestNameModifierChecker extends TestBase {

    @Test
    public void testClass () throws IOException {
	test ("class");
    }

    @Test
    public void testEnum () throws IOException {
	test ("enum");
    }

    @Test
    public void testInterface () throws IOException {
	test ("interface");
    }

    @Test
    public void testAnnotation () throws IOException {
	test ("@interface");
    }

    private void test (String type) throws IOException {
	parseAndSetClasses (type + " Foo {}");
	assertNoErrors ();
	parseAndSetClass ("Foo.java", "public " + type + " Foo {}");
	assertNoErrors ();
	parseAndSetClass ("Foo.java", "public " + type + " Bar {}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("protected " + type + " Foo {}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("private " + type + " Foo {}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testInnerClass () throws IOException {
	testInner ("class");
    }

    private void testInner (String type) throws IOException {
	parseAndSetClasses ("class Foo { " + type + " BAR {}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { public " + type + " BAR {}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { protected " + type + " BAR {}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { private " + type + " BAR {}}");
	assertNoErrors ();
    }

    @Test
    public void testMethod () throws IOException {
	testBody ("void bar () {}");
    }

    @Test
    public void testNativeStrictFPMethod () throws IOException {
	parseAndSetClasses ("class Foo { native void bar (); }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { strictfp void bar () {} }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { native strictfp void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testAbstractMethodFlags () throws IOException {
	// abstract may not be mixed with:
	// private, static, final, native, strictfp, or synchronized.
	parseAndSetClasses ("class Foo { abstract void bar (); }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { abstract private void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { abstract static void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { abstract final void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { abstract native void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { abstract strictfp void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { abstract synchronized void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testNoMethodBody () throws IOException {
	parseAndSetClasses ("class Foo { native void bar (); }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { abstract void bar (); }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { native void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo { abstract void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo { void bar (); }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo { synchronized void bar (); }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testInterfaceMethod () throws IOException {
	parseAndSetClasses ("interface Foo { void bar (); }");
	assertNoErrors ();
	parseAndSetClasses ("interface Foo { default void bar () {} }");
	assertNoErrors ();
	parseAndSetClasses ("interface Foo { static void bar () {} }");
	assertNoErrors ();

	parseAndSetClasses ("interface Foo { static void bar (); }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("interface Foo { default void bar (); }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("interface Foo { void bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testField () throws IOException {
	testBody ("int bar;");
    }

    @Test
    public void testFinalVolatileField () throws IOException {
	parseAndSetClasses ("class Foo { final int bar = 0; }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { volatile int bar = 0; }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { final volatile int bar = 0; }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testConstructor () throws IOException {
	testBody ("Foo () {}");
    }

    private void testBody (String bodyPart) throws IOException {
	parseAndSetClasses ("class Foo {" + bodyPart + "}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { public " + bodyPart + " }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { protected " + bodyPart + " }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { private " + bodyPart + " }");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { public private " + bodyPart + " }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { protected private " + bodyPart + " }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { public protected " + bodyPart + " }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { public protected private " + bodyPart + " }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testDuplicateFlags () throws IOException {
	testClassDups ("public");
	testClassDups ("protected");
	testClassDups ("private");
	testClassDups ("abstract");
	testClassDups ("final");
	testClassDups ("strictfp");

	testInnerClassDups ("static");
    }

    private void testClassDups (String flag) throws IOException {
	parseAndSetClasses (flag + " " + flag + " class Foo { }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
    }

    private void testInnerClassDups (String flag) throws IOException {
	parseAndSetClasses ("class Foo { " + flag + " " + flag + " class Bar {} }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testExtendsFinalExternal () throws IOException {
	parseAndSetClasses ("class Foo extends String {}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testExtendsFinalInternal () throws IOException {
	parseAndSetClasses ("final class Foo {}\n" +
			    "class Bar extends Foo {}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testExtendsInterface () throws IOException {
	parseAndSetClasses ("class Foo extends Runnable {}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    protected void handleSyntaxTree (SyntaxTree tree) {
	NameModifierChecker nmc = new NameModifierChecker (cip, tree, diagnostics);
	nmc.check ();
    }
}
