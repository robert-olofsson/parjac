package org.khelekore.parjac.parser;

import static org.khelekore.parjac.lexer.Token.*;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestPackage {
    private LRParser lr;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	lr = new LRParser (false);
	lr.addRule ("Goal", "CompilationUnit");
	lr.addRule ("CompilationUnit",
		    lr.zeroOrOne ("PackageDeclaration"));
	lr.addRule ("PackageDeclaration",
		    // Without the annotations
		    PACKAGE, IDENTIFIER, lr.zeroOrMore (DOT, IDENTIFIER), SEMICOLON);
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

    private void testSuccessfulParse (String s) {
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }

    private void testFailedParse (String s) {
	TestParseHelper.parse (lr, s, diagnostics);
	assert diagnostics.hasError () : "Failed to detect errors";
    }
}