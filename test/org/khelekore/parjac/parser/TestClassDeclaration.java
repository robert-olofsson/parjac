package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestClassDeclaration {
    private LRParser lr;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	Java8Grammar grammar = new Java8Grammar (false);
	lr = grammar.getLRParser ();
	lr.addRule ("Goal", "ClassDeclaration");
	grammar.addAllClassRules ();
	grammar.addTypeParameters ();
	grammar.addTypeRules ();
	grammar.addInterfaceRules ();
	grammar.addAnnotationRules ();
	grammar.addArrayInitializer ();
	grammar.addNameRules ();
	grammar.addLiteralRules ();

	// A bit simplified rules, but we are not testing these rules in this class
	lr.addRule ("Block", Token.LEFT_CURLY, lr.zeroOrMore ("BlockStatements"), Token.RIGHT_CURLY);
	lr.addRule ("BlockStatements", Token.SEMICOLON);
	lr.addRule ("ArgumentList", Token.IDENTIFIER);
	lr.addRule ("Primary", "Literal");
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
    public void testSimpleClasses () {
	testSuccessfulParse ("class Foo { }");
	testSuccessfulParse ("@Foo class Foo { }");
	testSuccessfulParse ("public class Foo { }");
	testSuccessfulParse ("protected class Foo { }");
	testSuccessfulParse ("private class Foo { }");
	testSuccessfulParse ("abstract class Foo { }");
	testSuccessfulParse ("final class Foo { }");
	testSuccessfulParse ("strictfp class Foo { }");
	testSuccessfulParse ("public final strictfp class Foo { }");
	testSuccessfulParse ("class Foo { private int foo; }");
	testSuccessfulParse ("class Foo { private int foo; \n" +
			     "public Foo () {}\n" +
			     "public int getFoo () {}\n" +
			     "private class Bar {}\n" +
			     "private enum Baz { ONE, TWO }\n" +
			     "}");
    }

    @Test
    public void testSimpleEnums () {
	testSuccessfulParse ("enum Foo { ONE, TWO, THREE }");
	testSuccessfulParse ("@Foo enum Foo { @Bar ONE, @Baz TWO, @Qux THREE }");
	testSuccessfulParse ("enum Foo { ONE, TWO, THREE; }");
	testSuccessfulParse ("public enum Foo { ONE, TWO, THREE; }");
	testSuccessfulParse ("private enum Foo { ONE, TWO, THREE; }");
	testSuccessfulParse ("enum Foo { ONE, TWO, THREE; \n" +
			     "private int foo; \n"+
			     "private Foo (int f) {}\n" +
			     "public void getFoo () { }\n" +
			     "}");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}