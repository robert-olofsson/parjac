package org.khelekore.parjac.parser;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
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
	testSuccessfulParse ("interface I { }");
	testSuccessfulParse ("public interface I { }");
	testSuccessfulParse ("public interface I<T> { }");
	testSuccessfulParse ("public interface I extends J { }");
	testSuccessfulParse ("public interface I extends J, K, L { }");
	testSuccessfulParse ("public interface I<T> extends J, K, L { }");
	testSuccessfulParse ("interface CP { void split (Parts parts); }");
	testSuccessfulParse ("interface CP { int i = 3; }");
	testSuccessfulParse ("interface CP { int i = 3, j = 4; boolean b(Foo foo, Bar bar);}");
	testSuccessfulParse ("interface CP { default void foo () { }}");
    }

    @Test
    public void testMultiClass () {
	testSuccessfulParse ("class A {} class B {} class C {}");
    }

    @Test
    public void testClasses () {
	testSuccessfulParse ("class A {}");
	testSuccessfulParse ("public final class A {}");
	testSuccessfulParse ("public final class A<T> {}");
	testSuccessfulParse ("public final class A extends B {}");
	testSuccessfulParse ("public final class A<T> extends B {}");
	testSuccessfulParse ("public final class A implements B {}");
	testSuccessfulParse ("public final class A implements B, C, D {}");
	testSuccessfulParse ("public final class A<T> implements B {}");
	testSuccessfulParse ("public final class A<T> extends B implements C {}");
    }

    @Test
    public void testAnnotation () {
	testSuccessfulParse ("@interface A {}");
	testSuccessfulParse ("public @interface A {}");
	testSuccessfulParse ("public @interface A {public int a ();}");
	testSuccessfulParse ("public @interface A {public int[] a ();}");
	testSuccessfulParse ("public @interface A {public int a () default 1;}");
    }

    @Test
    public void testEnum () {
	testSuccessfulParse ("enum A {}");
	testSuccessfulParse ("public enum A {}");
	testSuccessfulParse ("public enum A {;}");
	testSuccessfulParse ("public enum A {A, B, C}");
	testSuccessfulParse ("public enum A {A, B, C;}");
	testSuccessfulParse ("public enum A {A, B, C; void foo () {}}");
    }

    private void testSuccessfulParse (String s) {
	testSuccessfulParse (s, null);
    }

    private void testSuccessfulParse (String s, TreeNode tn) {
	SyntaxTree t = TestParseHelper.earleyParseBuildTree (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParseHelper.getParseOutput (diagnostics);
	if (tn != null)
	    assert tn.equals (t.getRoot ()) : "Got unexpected tree: " + t.getRoot () + ", expected: " + tn;
    }
}
