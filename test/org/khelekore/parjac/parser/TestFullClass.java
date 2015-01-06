package org.khelekore.parjac.parser;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestFullClass {
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
    public void testEmpty () {
	testSuccessfulParse ("");
    }

    @Test
    public void testHelloWorld () {
	testSuccessfulParse ("class HW { public static void main (String... args) { S.o.p(\"HW!\"); } }");
	testSuccessfulParse ("class HW { public static void main (String[] args) { S.o.p(\"HW!\"); } }");
    }

    @Test
    public void testInterface () {
	testSuccessfulParse ("interface CP { void split (List<List<SP>> parts); }");
    }

    @Test
    public void testMultiClass () {
	testSuccessfulParse ("class A {} class B {} class C {}");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.earleyParse (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}
