package org.khelekore.parjac.semantics;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
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
    public void testInnerClassShadowingFieldLast () throws IOException {
	parseAndSetClasses ("class Foo { class Bar { int a; } int a; }");
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
    public void testStaticNoShadow () throws IOException {
	parseAndSetClasses ("class Foo { int a; static void foo () { int a = 0; }}");
	assertNoErrors ();
	assertNoWarnings ();
    }

    @Test
    public void testStaticShadow () throws IOException {
	parseAndSetClasses ("class Foo { static int a; static void foo () { int a = 0; }}");
	assertNoErrors ();
	assert diagnostics.hasWarning () : "Expected to find shadowing warning";
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
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo { void foo () { int i; for (int i = 0; i < 3; i++) {}}}");
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
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("import java.util.Collections;\n" +
			    "import java.util.List;\n" +
			    "class Foo {\n" +
			    "    List<String> ls = Collections.emptyList ();\n" +
			    "    void foo () { String s; for (String s : ls) { }}}");
	assert diagnostics.hasError () : "Expected duplicate variables";
    }

    @Test
    public void testShadowing () throws IOException {
	parseAndSetClasses ("class Foo { int a; void foo () { int a = 3; }}");
	assertNoErrors ();
	assert diagnostics.hasWarning () : "Expected to find shadowing warning";
    }

    @Test
    public void testFieldAndMethodArgumentName () throws IOException {
	parseAndSetClasses ("class F { int i; void f (int i) {}}");
	assertNoErrors ();
    }

    @Test
    public void testBlockNameClash () throws IOException {
	parseAndSetClasses ("class Foo { void foo () { int a; { int a; }}}");
	assert diagnostics.hasError () : "Expected name already in use";
    }

    @Test
    public void testSameNameInInnerClass () throws IOException {
	parseAndSetClasses ("class F { int i; class G { int i; }}");
	assertNoErrors ();
    }

    @Test
    public void testClassNames () throws IOException {
	parseAndSetClasses ("class Foo { class Foo {}}");
	assert diagnostics.hasError () : "Expected illegal name";
    }
}
