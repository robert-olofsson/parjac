package org.khelekore.parjac.parser;

import java.util.Arrays;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.tree.DottedName;
import org.khelekore.parjac.tree.MarkerAnnotation;
import org.khelekore.parjac.tree.PackageDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestPackage {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParseHelper.getJavaGrammarFromFile ("PackageDeclaration", false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSinglePackage () {
	testSuccessfulParse ("package foo;",
			     new PackageDeclaration (null, new DottedName ("foo"), null));
    }

    @Test
    public void testSinglePackageMissingSemiColon () {
	testFailedParse ("package foo");
    }

    @Test
    public void testMultiPackage () {
	testSuccessfulParse ("package foo.bar.baz;",
			     new PackageDeclaration (null, new DottedName ("foo", "bar", "baz"), null));
    }

    @Test
    public void testMarkerAnnotatedPackage () {
	DottedName dn = new DottedName ("foo");
	DottedName bar = new DottedName ("Bar");
	testSuccessfulParse ("@foo package foo;",
			     new PackageDeclaration (Arrays.asList (new MarkerAnnotation (dn, null)), dn, null));
	testSuccessfulParse ("@foo @Bar package foo;",
			     new PackageDeclaration (Arrays.asList (new MarkerAnnotation (dn, null),
								    new MarkerAnnotation (bar, null)), dn, null));
    }

    @Test
    public void testSingleElementAnnotatedPackage () {
	testSuccessfulParse ("@foo(bar) package foo;");
    }

    @Test
    public void testNormalAnnotatedPackage () {
	testSuccessfulParse ("@foo() package foo;");
    }

    private void testSuccessfulParse (String s) {
	testSuccessfulParse (s, null);
    }

    private void testSuccessfulParse (String s, TreeNode tn) {
	SyntaxTree t = TestParseHelper.earleyParseBuildTree (g, s, null, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
	if (tn != null)
	    assert tn.equals (t.getRoot ()) : "Got unexpected tree: " + t.getRoot () + ", expected: " + tn;
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