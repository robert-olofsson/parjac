package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestFieldDeclaration {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParseHelper.getJavaGrammarFromFile ("FieldDeclaration", false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSimpleFields () {
	testSuccessfulParse ("int i;");
	testSuccessfulParse ("int i = a;");
	testSuccessfulParse ("@Foo Bar b;");
    }

    @Test
    public void testSimpleModifiedFields () {
	testSuccessfulParse ("public int i;");
	testSuccessfulParse ("protected int i = a;");
	testSuccessfulParse ("final @Foo Bar b;");
    }

    @Test
    public void testArrays () {
	testSuccessfulParse ("int arr[];");
	testSuccessfulParse ("int arr[][][];");
	testSuccessfulParse ("int arr[] = {};");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.earleyParse (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}