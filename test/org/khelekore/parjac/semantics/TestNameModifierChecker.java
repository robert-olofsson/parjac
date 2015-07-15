package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.parser.TestParseHelper;
import org.khelekore.parjac.tree.SyntaxTree;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestNameModifierChecker {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () throws IOException {
	g = TestParseHelper.getJavaGrammarFromFile ("CompilationUnit", false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testClass () throws IOException {
	test ("class");
    }

    @Test
    public void testEnum () throws IOException {
	test ("enum");
    }

    @Test
    public void testInterface () throws IOException {
	test ("interface");
    }

    @Test
    public void testAnnotation () throws IOException {
	test ("@interface");
    }

    private void test (String type) throws IOException {
	parseAndSetClasses (type + " Foo {}");
	assertNoErrors ();
	parseAndSetClasses ("public " + type + " Foo {}");
	assertNoErrors ();
	parseAndSetClasses ("public " + type + " Bar {}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("protected " + type + " Foo {}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("private " + type + " Foo {}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testInnerClass () throws IOException {
	testInner ("class");
    }

    private void testInner (String type) throws IOException {
	parseAndSetClasses ("class Foo { " + type + " BAR {}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { public " + type + " BAR {}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { protected " + type + " BAR {}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { private " + type + " BAR {}}");
	assertNoErrors ();
    }

    @Test
    public void testMethod () throws IOException {
	testBody ("void bar () {}");
    }

    @Test
    public void testField () throws IOException {
	testBody ("int bar;");
    }

    @Test
    public void testConstructor () throws IOException {
	testBody ("Foo () {}");
    }

    private void testBody (String bodyPart) throws IOException {
	parseAndSetClasses ("class Foo {" + bodyPart + "}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { public " + bodyPart + " }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { protected " + bodyPart + " }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { private " + bodyPart + " }");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { public private " + bodyPart + " }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { protected private " + bodyPart + " }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { public protected " + bodyPart + " }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { public protected private " + bodyPart + " }");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
    }

    private void parseAndSetClasses (String code) {
	SyntaxTree st = TestParseHelper.earleyParseBuildTree (g, code, "Foo.java", diagnostics);
	assert st != null : "Failed to parse:"  + code + ": " + getDiagnostics ();
	NameModifierChecker nmc = new NameModifierChecker (st, diagnostics);
	nmc.check ();
    }

    private void assertNoErrors () {
 	assert !diagnostics.hasError () : "Got errors: " + getDiagnostics ();
    }

    private String getDiagnostics () {
	Locale loc = Locale.getDefault ();
	return diagnostics.getDiagnostics ().
	    map (c -> c.getMessage (loc)).
	    collect (Collectors.joining ("\n"));
    }
}
