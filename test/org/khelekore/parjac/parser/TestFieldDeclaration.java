package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestFieldDeclaration {
    private LRParser lr;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	Java8Grammar grammar = new Java8Grammar (false);
	lr = grammar.getLRParser ();
	lr.addRule ("Goal", "FieldDeclaration");
	grammar.addFieldDeclaration ();
	grammar.addModifiers ();
	grammar.addNameRules ();
	grammar.addTypeRules ();
	grammar.addUnannTypes ();
	grammar.addAnnotationRules ();
	grammar.addArrayInitializer ();
	// simplified
	lr.addRule ("Expression", Token.IDENTIFIER);
	lr.addRule ("ConditionalExpression", Token.IDENTIFIER);
	try {
	    lr.build ();
	} catch (Throwable t) {
	    t.printStackTrace ();
	}
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
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}