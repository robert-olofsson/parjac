package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.parser.TestParseHelper;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestClassSetter {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;
    private CompiledTypesHolder cth;
    private ClassResourceHolder crh;

    @BeforeClass
    public void createLRParser () throws IOException {
	g = TestParseHelper.getJavaGrammarFromFile ("CompilationUnit", false);
	crh = new ClassResourceHolder ();
	crh.scanClassPath ();
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
	cth = new CompiledTypesHolder ();
    }

    @Test
    public void testExtendsNonExisting () throws IOException {
	parseAndSetClasses ("class Bar extends QWER {}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testExtendsThread () throws IOException {
	parseAndSetClasses ("class Foo extends Thread {}");
	assertNoErrors ();
	checkSuperTypeName ("Foo", "java.lang.Thread");
    }

    @Test
    public void testImplementsNonExisting () throws IOException {
	parseAndSetClasses ("class Foo implements QWER {}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testImplementsRunnable () throws IOException {
	parseAndSetClasses ("class Foo implements Runnable {}");
	assertNoErrors ();
    }

    @Test
    public void testMultiClass () throws IOException {
	parseAndSetClasses ("class Foo {} class Bar extends Foo {}");
	assertNoErrors ();
    }

    @Test
    public void testFieldOfSameClass () throws IOException {
	parseAndSetClasses ("package foo.bar; class Foo { Foo next; }");
	assertNoErrors ();
	checkField ("foo.bar.Foo", 0, "foo.bar.Foo");
    }

    @Test
    public void testExtendsWithPackage () throws IOException {
	parseAndSetClasses ("package foo.bar; class Foo {} class Bar extends Foo {}");
	assertNoErrors ();
	checkSuperTypeName ("foo.bar.Bar", "foo.bar.Foo");
    }

    @Test
    public void testInnerClass () throws IOException {
	parseAndSetClasses ("package foo.bar; class Foo { class Bar {} Bar bar;}");
	assertNoErrors ();
	checkField ("foo.bar.Foo", 1, "foo.bar.Foo.Bar");
    }

    private void parseAndSetClasses (String code) {
	SyntaxTree st = TestParseHelper.earleyParseBuildTree (g, code, diagnostics);
	cth.addTypes (st);
	ClassSetter cs = new ClassSetter (cth, crh, st, diagnostics);
	cs.fillIn ();
    }

    private void checkSuperTypeName (String classToCheck, String expectedSuperType) {
	NormalClassDeclaration cd = (NormalClassDeclaration)cth.getType (classToCheck);
	assertClassType (cd.getSuperClass (), expectedSuperType);
    }

    private void checkField (String className, int bodyPosition, String expectedType) {
	NormalClassDeclaration cd = (NormalClassDeclaration)cth.getType (className);
	ClassBody body = cd.getBody ();
	FieldDeclaration fd = (FieldDeclaration)body.getDeclarations ().get (bodyPosition);
	assertClassType ((ClassType)fd.getType (), expectedType);
    }

    private void assertClassType (ClassType ct, String expectedType) {
	assert ct.getFullName ().equals (expectedType) : "Got wrong type: " +
	    ct.getFullName () + ", expected: " + expectedType;
    }

    private void assertNoErrors () {
	assert !diagnostics.hasError () : "Got errors: " +
	    diagnostics.getDiagnostics ().map (c -> c.toString ()).collect (Collectors.joining ("\n"));
    }
}