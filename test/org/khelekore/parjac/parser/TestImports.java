package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestImports {
    private LRParser lr;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	Java8Grammar grammar = new Java8Grammar (false);
	lr = grammar.getLRParser ();
	lr.addRule ("Goal", "CompilationUnit");
	lr.addRule ("CompilationUnit",
		    lr.zeroOrMore ("ImportDeclaration"));
	grammar.addNameRules ();
	grammar.addImportRules ();
	lr.build ();
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
    public void testSingleTypeImportDeclarationClass () {
	testSuccessfulParse ("import Random;");
    }

    @Test
    public void testSingleTypeImportDeclarationPackages () {
	testSuccessfulParse ("import java.util.Random;");
    }

    @Test
    public void testTypeImportOnDemandDeclarationOnePackage () {
	testSuccessfulParse ("import java.*;");
    }

    @Test
    public void testTypeImportOnDemandDeclarationManyPackages () {
	testSuccessfulParse ("import java.util.*;");
    }

    @Test
    public void testSingleStaticImportDeclaration () {
	testSuccessfulParse ("import static java.lang.Math.abs;");
    }

    @Test
    public void testStaticImportOnDemandDeclaration () {
	testSuccessfulParse ("import static java.lang.Math.*;");
    }

    @Test
    public void testMany () {
	testSuccessfulParse ("import Foo;\n" +
			     "import Bar;\n" +
			     "import foo.bar.Baz;" +
			     "import foo.bar.baz.*;");
    }

    @Test
    public void testMissingSemiColon () {
	testFailedParse ("import Foo");
    }

    @Test
    public void testExtraStar () {
	testFailedParse ("import foo.*.*;");
    }

    @Test
    public void testStarIdentifier () {
	testFailedParse ("import *.Foo;");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }

    private void testFailedParse (String s) {
	try {
	    TestParseHelper.parse (lr, s, diagnostics);
	    assert diagnostics.hasError () : "Failed to detect errors";
	} finally {
	    diagnostics.clear ();
	}
    }
}