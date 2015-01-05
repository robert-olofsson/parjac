package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestTypes {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParseHelper.getJavaGrammarFromFile ("Type", true);
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testEmpty () {
	testSuccessfulParse ("");
    }

    @Test
    public void testSimpleBasic () {
	testSuccessfulParse ("byte");
	testSuccessfulParse ("short");
	testSuccessfulParse ("int");
	testSuccessfulParse ("long");
	testSuccessfulParse ("float");
	testSuccessfulParse ("double");
	testSuccessfulParse ("boolean");
	testSuccessfulParse ("Foo");
    }

    @Test
    public void testLongName () {
	testSuccessfulParse ("foo.Bar");
	testSuccessfulParse ("foo.bar.Baz");
	testSuccessfulParse ("foo.bar.baz.Quod");
    }

    @Test
    public void testAnnotated () {
	testSuccessfulParse ("@foo byte");
	testSuccessfulParse ("@foo(bar) byte");
	testSuccessfulParse ("@foo(bar={a, b, c}) byte");
	testSuccessfulParse ("@foo(@bar) byte");
    }

    @Test
    public void testArrays () {
	testSuccessfulParse ("int[]");
	testSuccessfulParse ("int[][]");
	testSuccessfulParse ("int[][][]");
    }

    @Test
    public void testTyped () {
	testSuccessfulParse ("Foo<T>");
	testSuccessfulParse ("Foo<K, V>");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.earleyParse (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}