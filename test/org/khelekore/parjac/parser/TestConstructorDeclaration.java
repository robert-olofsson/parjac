package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestConstructorDeclaration {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	Java8Grammar grammar = new Java8Grammar (false);
	grammar.addConstructorDeclaration ();
	grammar.addModifiers ();
	grammar.addFormalParameterList ();
	grammar.addThrows ();
	grammar.addLiteralRules ();
	grammar.addFieldDeclaration ();
	grammar.addNameRules ();
	grammar.addTypeRules ();
	grammar.addTypeParameters ();
	grammar.addUnannTypes ();
	grammar.addAnnotationRules ();
	grammar.addArrayInitializer ();

	g = grammar.getGrammar ();
	g.addRule ("Goal", "ConstructorDeclaration", Token.END_OF_INPUT);
	// A bit simplified rules, but we are not testing these rules in this class
	g.addRule ("BlockStatements", Token.SEMICOLON);
	g.addRule ("ArgumentList", Token.IDENTIFIER);
	g.addRule ("Primary", "Literal");
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
    public void testSimpleConstructor () {
	testSuccessfulParse ("Foo () {}");
	testSuccessfulParse ("Foo () {super();}");
	testSuccessfulParse ("Foo (int a) {super();}");
	testSuccessfulParse ("Foo (int a) {super(a);}");
	testSuccessfulParse ("Foo (int a) {this();}");
	testSuccessfulParse ("Foo (int a) {this(a);}");
    }

    @Test
    public void testAnnotatedConstructor () {
	testSuccessfulParse ("@Bar Foo () {}");
	testSuccessfulParse ("@Bar @Baz Foo () {}");
    }

    @Test
    public void testModifiersConstructor () {
	testSuccessfulParse ("public Foo () {}");
	testSuccessfulParse ("protected Foo () {}");
	testSuccessfulParse ("private Foo () {}");

	testSuccessfulParse ("@Bar public Foo () {}");
	testSuccessfulParse ("@Bar protected Foo () {}");
	testSuccessfulParse ("@Bar private Foo () {}");
    }

    @Test
    public void testConstructorThatThrows () {
	testSuccessfulParse ("Foo () throws A {}");
	testSuccessfulParse ("Foo () throws A, B {}");
    }

    @Test
    public void testGenericConstructor () {
	testSuccessfulParse ("<T> Foo () {}");
	testSuccessfulParse ("<T> Foo (T t) {}");
	testSuccessfulParse ("<K, V> Foo () {}");
    }

    @Test
    public void testGenericInvocations () {
	testSuccessfulParse ("Foo () {<T>this();}");
	testSuccessfulParse ("Foo () {<T>super();}");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.earleyParse (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}