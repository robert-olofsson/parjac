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
	lr.addRule ("Goal", "TypeDeclaration");
	grammar.addLiteralRules ();
	grammar.addTypeRules ();
	grammar.addNameRules ();
	grammar.addTypeDeclaration ();
	grammar.addAllClassRules ();
	grammar.addInterfaceRules ();
	grammar.addAnnotationRules ();
	grammar.addArrayInitializer ();

	// A bit simplified rules, but we are not testing these rules in this class
	lr.addRule ("Block", Token.LEFT_CURLY, lr.zeroOrMore ("BlockStatements"), Token.RIGHT_CURLY);
	lr.addRule ("BlockStatements", Token.SEMICOLON);
	lr.addRule ("ArgumentList", Token.IDENTIFIER);
	lr.addRule ("Primary", "Literal");
	lr.addRule ("Expression", lr.oneOf (Token.IDENTIFIER, "Literal"));
	lr.addRule ("ConditionalExpression", lr.oneOf (Token.IDENTIFIER, "Literal"));
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

    @Test
    public void testInterfaces () {
	testSuccessfulParse ("interface Foo {}");
	testSuccessfulParse ("public interface Foo {}");
	testSuccessfulParse ("protected interface Foo {}");
	testSuccessfulParse ("private interface Foo {}");
	testSuccessfulParse ("@Foo interface Foo {}");
	testSuccessfulParse ("interface Foo { public static final int FOO; }");
	testSuccessfulParse ("interface Foo { void foo (); }");
	testSuccessfulParse ("interface Foo { default void foo () { } }");
	testSuccessfulParse ("interface Foo { public void foo (); }");
	testSuccessfulParse ("interface Foo { public static final int FOO; public void foo (); }");
    }

    @Test
    public void testAnnotation () {
	testSuccessfulParse ("@interface Foo {}");
	testSuccessfulParse ("public @interface Foo {}");
	testSuccessfulParse ("@Foo @interface Foo {}");

	testSuccessfulParse ("@interface ClassPreamble {\n" +
			     "String author();\n" +
			     "String date();\n" +
			     "int currentRevision() default 1;\n" +
			     "String lastModified() default \"N/A\";\n" +
			     "String lastModifiedBy() default \"N/A\";\n" +
			     "// Note use of array\n" +
			     "String[] reviewers();\n" +
			     "}");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}