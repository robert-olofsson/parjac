package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.java8.Java8Grammar;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestBlock {
    private LRParser lr;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	Java8Grammar grammar = new Java8Grammar (false);
	lr = grammar.getLRParser ();
	lr.addRule ("Goal", "Block");
	grammar.addAllRules ();
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
    public void testEmptyBlock () {
	testSuccessfulParse ("{ }");
	testSuccessfulParse ("{ ; }");
	testSuccessfulParse ("{ ;;; }");
    }

    @Test
    public void testSimpleBlock () {
	testSuccessfulParse ("{ int a = b + c; }");
    }

    @Test
    public void testFor () {
	testSuccessfulParse ("{ for (int i = 0; i < 10; i++) {\n" +
			     "System.out.println (\"i: \"+ i);\n" +
			     "}\n}");
	/* Currently failing */
	/*
	testSuccessfulParse ("{ for (Foo foo : listOfFoo) {\n" +
			     "System.out.println (\"foo: \"+ foo);\n" +
			     "}\n}");
	*/
    }

    @Test
    public void testIf () {
	testSuccessfulParse ("{ if (true) a = b; }");
	testSuccessfulParse ("{ if (true) return a; }");
	testSuccessfulParse ("{ if (true) return a; else return b; }");
	testSuccessfulParse ("{ if (true) { return a; } else { return b; }}");
    }

    @Test
    public void testWhile () {
	testSuccessfulParse ("{ while (true) { i++; }}");
	testSuccessfulParse ("{ do { i++; } while (true); }");
    }

    @Test
    public void testSwitch () {
	testSuccessfulParse ("{ switch (foo) {\n" +
			     "default: a = b; }}");
	testSuccessfulParse ("{ switch (foo) {\n" +
			     "case 1: a = foo; break;\n" +
			     "case 2: a = bar; break;\n" +
			     "case 3: a = baz; break;\n" +
			     "}}");
	/* Currently failing */
	/*
	testSuccessfulParse ("{ switch (foo) {\n" +
			     "case 1: a = b; break;\n" +
			     "case 2: b = 2; break;\n" +
			     "default: c = 3; }}");
	testSuccessfulParse ("{ switch (foo) {\n" +
			     "case A: return 1;\n" +
			     "case B: case C: return 2;\n" +
			     "default: return 3;\n" +
			     "}}");
	*/
    }

    @Test
    public void testReturn () {
	testSuccessfulParse ("{return true;}");
	testSuccessfulParse ("{return 1;}");
	testSuccessfulParse ("{return 1.0;}");
	testSuccessfulParse ("{return a || b;}");
	testSuccessfulParse ("{return 2 * 3;}");
	testSuccessfulParse ("{return 2 + 3;}");
	testSuccessfulParse ("{return 2 & 3;}");
    }

    @Test
    public void testLocalVariableDeclaration () {
	testSuccessfulParse ("{int i;}");
	testSuccessfulParse ("{int i, j;}");
	testSuccessfulParse ("{int[] i, j;}");
	testSuccessfulParse ("{int[] i[], j[][];}");
	testSuccessfulParse ("{int i = 1;}");
	testSuccessfulParse ("{int i = a;}");
	testSuccessfulParse ("{int i = 1 + 2;}");
	testSuccessfulParse ("{int i = 1 + a;}");
	/* Currently failing */
	// testSuccessfulParse ("{int i;\nFoo foo;\nBar baz;\ndouble d; }");
    }

    @Test
    public void testSynchronized () {
	testSuccessfulParse ("{synchronized (foo) { i = 0; }}");
    }

    @Test
    public void testThrow () {
	testSuccessfulParse ("{throw e;}");
	testSuccessfulParse ("{throw new FooException(\"blargle\");}");
    }

    @Test
    public void testTry () {
	testSuccessfulParse ("{try { a = b; } finally { c = d; }}");
	testSuccessfulParse ("{try { a = b; } catch (AException a) { } finally { c = d; }}");
	testSuccessfulParse ("{try { a = b; } catch (A | B ex) { } finally { c = d; }}");

	testSuccessfulParse ("{try (Foo foo = new Foo()) { a = b; } }");
	testSuccessfulParse ("{try (Foo foo = new Foo(); Bar bar = new Bar(foo)) { a = b; } }");
    }

    @Test
    public void testAssert () {
	testSuccessfulParse ("{ assert foo; }");
	testSuccessfulParse ("{ assert foo : \"lorem...\"; }");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}