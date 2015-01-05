package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestPackage {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParseHelper.getJavaGrammarFromFile ("PackageDeclaration", false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSinglePackage () {
	testSuccessfulParse ("package foo;");
    }

    @Test
    public void testSinglePackageMissingSemiColon () {
	testFailedParse ("package foo");
    }

    @Test
    public void testMultiPackage () {
	testSuccessfulParse ("package foo.bar.baz;");
    }

    @Test
    public void testMarkerAnnotatedPackage () {
	testSuccessfulParse ("@foo package foo;");
    }

    @Test
    public void testSingleElementAnnotatedPackage () {
	testSuccessfulParse ("@foo(bar) package foo;");
    }

    @Test
    public void testNormalAnnotatedPackage () {
	testSuccessfulParse ("@foo() package foo;");
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