package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestTypes {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	Java8Grammar grammar = new Java8Grammar (false);
	grammar.addTypeRules ();
	grammar.addNameRules ();
	grammar.addAnnotationRules ();
	g = grammar.getGrammar ();
	g.addRule ("Goal", "Types", Token.END_OF_INPUT);
	g.addRule ("Types", g.zeroOrMore ("Type"));
	// Just make it something, CE will be tested in its own test
	g.addRule ("ConditionalExpression", Token.IDENTIFIER);
	try  {
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