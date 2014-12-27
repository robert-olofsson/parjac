package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestFieldDeclaration {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	Java8Grammar grammar = new Java8Grammar (false);
	grammar.addFieldDeclaration ();
	grammar.addModifiers ();
	grammar.addNameRules ();
	grammar.addTypeRules ();
	grammar.addUnannTypes ();
	grammar.addAnnotationRules ();
	grammar.addArrayInitializer ();

	g = grammar.getGrammar ();
	g.addRule ("Goal", "FieldDeclaration", Token.END_OF_INPUT);
	// simplified
	g.addRule ("Expression", Token.IDENTIFIER);
	g.addRule ("ConditionalExpression", Token.IDENTIFIER);
	try {
	    g.validateRules ();
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
	TestParseHelper.earleyParse (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}