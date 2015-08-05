package org.khelekore.parjac.semantics;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.tree.SyntaxTree;
import org.testng.annotations.Test;

public class TestNames extends TestBase {

    @Test
    public void testKnownLocal () throws IOException {
	parseAndSetClasses ("class Foo { void foo () { int a = 3; }}");
	assertNoErrors ();
    }

    @Test
    public void testDuplicateLocal () throws IOException {
	parseAndSetClasses ("class Foo { void foo () { int a = 3; int a = 4; }}");
	assert diagnostics.hasError () : "Expected duplicate variables";
    }

    @Test
    public void testKnownField () throws IOException {
	parseAndSetClasses ("class Foo { int a; void foo () { a = 3; }}");
	assertNoErrors ();
    }

    @Test
    public void testDuplicateField () throws IOException {
	parseAndSetClasses ("class Foo { int a; int a; void foo () {}}");
	assert diagnostics.hasError () : "Expected duplicate variables";
    }

    @Test
    public void testForVariable () throws IOException {
	parseAndSetClasses ("class Foo { void foo () { for (int i = 0; i < 2; i++) {int j = i * i;}}}");
	assertNoErrors ();
    }

    @Test
    public void testStaticFieldAccess () throws IOException {
	parseAndSetClasses ("class Bar { public static int bar = 3; }",
			    "class Foo { void foo () { int i = Bar.bar; }}");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassShadowing () throws IOException {
	parseAndSetClasses ("class Foo { int a; class Bar { int a; }}");
	assertNoErrors ();
	assert diagnostics.hasWarning () : "Expected to find shadowing warning";
    }

    @Test
    public void testInnerClass () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "void foo () {\n" +
			    "    int a = 3;\n" +
			    "    Runnable r = new Runnable () {\n" +
			    "        public void run () { int b = a; }};\n" +
			    "}}");
	assertNoErrors ();
    }

    @Test
    public void testBasicFor () throws IOException {
	parseAndSetClasses ("class Foo { void foo () { for (int i = 0; i < 3; i++) { int j = i; }}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { void foo () { for (int i = 0, j = 0; i < 3; i++) { int k = i + j; }}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { void foo () { for (int i = 0; i < 3; i++) { int i = 17; }}}");
	assert diagnostics.hasError () : "Expected duplicate variables";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo { void foo () { for (int i = 0, j = 0; i < 3; i++) { int j = i; }}}");
	assert diagnostics.hasError () : "Expected duplicate variables";
    }

    @Test
    public void testEnhancedFor () throws IOException {
	parseAndSetClasses ("import java.util.Collections;\n" +
			    "import java.util.List;\n" +
			    "class Foo {\n" +
			    "    List<String> ls = Collections.emptyList ();\n" +
			    "    void foo () { for (String s : ls) { int i = 0; }}}");
	assertNoErrors ();
	parseAndSetClasses ("import java.util.Collections;\n" +
			    "import java.util.List;\n" +
			    "class Foo {\n" +
			    "    List<String> ls = Collections.emptyList ();\n" +
			    "    void foo () { for (String s : ls) { int s = 0; }}}");
	assert diagnostics.hasError () : "Expected duplicate variables";
    }

    @Test
    public void testShadowing () throws IOException {
	parseAndSetClasses ("class Foo { void foo () { int a = 3; { int a = 5;}}}");
	assertNoErrors ();
	assert diagnostics.hasWarning () : "Expected to find shadowing warning";
    }

    protected void handleSyntaxTree (SyntaxTree tree) {
	FieldAndMethodScanner fams = new FieldAndMethodScanner (tree, diagnostics);
	fams.scan ();
    }
}
