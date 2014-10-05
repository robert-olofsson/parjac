package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestTypes {
    private LRParser lr;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	Java8Grammar grammar = new Java8Grammar (true);
	lr = grammar.getLRParser ();
	lr.addRule ("Goal", "Types");
	lr.addRule ("Types",
		    lr.zeroOrMore ("Type"));
	grammar.addTypeRules ();
	grammar.addNameRules ();
	grammar.addAnnotationRules ();
	// Just make it something, CE will be tested in its own test
	lr.addRule ("ConditionalExpression",
		    Token.IDENTIFIER);
	try  {
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
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}