package org.khelekore.parjac.parser;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestImports {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParseHelper.getJavaGrammarFromFile ("ImportDeclaration", true);
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
    public void testSingleTypeImportDeclarationClass () {
	testSuccessfulParse ("import Random;");
    }

    @Test
    public void testSingleTypeImportDeclarationPackages () {
	testSuccessfulParse ("import java.util.Random;");
    }

    @Test
    public void testTypeImportOnDemandDeclarationOnePackage () {
	testSuccessfulParse ("import java.*;");
    }

    @Test
    public void testTypeImportOnDemandDeclarationManyPackages () {
	testSuccessfulParse ("import java.util.*;");
    }

    @Test
    public void testSingleStaticImportDeclaration () {
	testSuccessfulParse ("import static java.lang.Math.abs;");
    }

    @Test
    public void testStaticImportOnDemandDeclaration () {
	testSuccessfulParse ("import static java.lang.Math.*;");
    }

    @Test
    public void testMany () {
	testSuccessfulParse ("import Foo;\n" +
			     "import Bar;\n" +
			     "import foo.bar.Baz;" +
			     "import foo.bar.baz.*;");
    }

    @Test
    public void testMissingSemiColon () {
	testFailedParse ("import Foo");
    }

    @Test
    public void testExtraStar () {
	testFailedParse ("import foo.*.*;");
    }

    @Test
    public void testStarIdentifier () {
	testFailedParse ("import *.Foo;");
    }

    private void testSuccessfulParse (String s) {
	testSuccessfulParse (s, null);
    }

    private void testSuccessfulParse (String s, TreeNode tn) {
	SyntaxTree t = TestParseHelper.earleyParseBuildTree (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
	if (tn != null)
	    assert tn.equals (t.getCompilationUnit ()) :
	    "Got unexpected tree: " + t.getCompilationUnit () + ", expected: " + tn;
    }

    private void testFailedParse (String s) {
	try {
	    TestParseHelper.earleyParse (g, s, diagnostics);
	    assert diagnostics.hasError () : "Failed to detect errors";
	} finally {
	    diagnostics.clear ();
	}
    }
}