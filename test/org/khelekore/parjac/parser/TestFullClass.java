package org.khelekore.parjac.parser;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.GrammarReader;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestFullClass {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () throws IOException {
	GrammarReader gr = new GrammarReader (false);
	gr.read (getClass ().getResource ("/java_8.pj"));
	g = gr.getGrammar ();
	g.addRule ("Goal", "CompilationUnit", Token.END_OF_INPUT);
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
    public void testHelloWorld () {
	testSuccessfulParse ("class HW { public static void main (String... args) { S.o.p(\"HW!\"); } }");
	testSuccessfulParse ("class HW { public static void main (String[] args) { S.o.p(\"HW!\"); } }");
    }

    private void testSuccessfulParse (String s) {
	TestParseHelper.earleyParse (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
    }
}
