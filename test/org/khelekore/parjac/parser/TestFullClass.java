package org.khelekore.parjac.parser;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.GrammarReader;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestFullClass {
    private LRParser lr;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () throws IOException {
	GrammarReader gr = new GrammarReader (false);
	gr.read (getClass ().getResource ("/java_8.pj"));
	lr = gr.getParser ();
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
    public void testHelloWorld () {
	testSuccessfulParse ("class HW { public static void main (String... args) { S.o.p(\"HW!\"); } }");
	testSuccessfulParse ("class HW { public static void main (String[] args) { S.o.p(\"HW!\"); } }");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.parse (lr, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}
