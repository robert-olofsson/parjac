package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestConstructorDeclaration {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParseHelper.getJavaGrammarFromFile ("ConstructorDeclaration", false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSimpleConstructor () {
	testSuccessfulParse ("Foo () {}");
	testSuccessfulParse ("Foo () {super();}");
	testSuccessfulParse ("Foo (int a) {super();}");
	testSuccessfulParse ("Foo (int a) {super(a);}");
	testSuccessfulParse ("Foo (int a) {this();}");
	testSuccessfulParse ("Foo (int a) {this(a);}");
    }

    @Test
    public void testAnnotatedConstructor () {
	testSuccessfulParse ("@Bar Foo () {}");
	testSuccessfulParse ("@Bar @Baz Foo () {}");
    }

    @Test
    public void testModifiersConstructor () {
	testSuccessfulParse ("public Foo () {}");
	testSuccessfulParse ("protected Foo () {}");
	testSuccessfulParse ("private Foo () {}");

	testSuccessfulParse ("@Bar public Foo () {}");
	testSuccessfulParse ("@Bar protected Foo () {}");
	testSuccessfulParse ("@Bar private Foo () {}");
    }

    @Test
    public void testConstructorThatThrows () {
	testSuccessfulParse ("Foo () throws A {}");
	testSuccessfulParse ("Foo () throws A, B {}");
    }

    @Test
    public void testGenericConstructor () {
	testSuccessfulParse ("<T> Foo () {}");
	testSuccessfulParse ("<T> Foo (T t) {}");
	testSuccessfulParse ("<K, V> Foo () {}");
    }

    @Test
    public void testGenericInvocations () {
	testSuccessfulParse ("Foo () {<T>this();}");
	testSuccessfulParse ("Foo () {<T>super();}");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.earleyParse (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}