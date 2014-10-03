package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestPackage {
    private LRParser lr;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	try {
	Java8Grammar grammar = new Java8Grammar (false);
	lr = grammar.getLRParser ();
	lr.addRule ("Goal", "CompilationUnit");
	lr.addRule ("CompilationUnit",
		    lr.zeroOrOne ("PackageDeclaration"));
	grammar.addNameRules ();
	grammar.addPackageRules ();
	grammar.addAnnotationRules ();
	// Just make it something, CE will be tested in its own test
	lr.addRule ("ConditionalExpression",
		    Token.IDENTIFIER);
	lr.build ();
	} catch (Throwable t) {
	    t.printStackTrace();
	}
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
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }

    private void testFailedParse (String s) {
	TestParseHelper.parse (lr, s, diagnostics);
	assert diagnostics.hasError () : "Failed to detect errors";
    }
}