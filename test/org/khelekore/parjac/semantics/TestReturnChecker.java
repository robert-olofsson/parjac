package org.khelekore.parjac.semantics;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.tree.SyntaxTree;
import org.testng.annotations.Test;

public class TestReturnChecker extends TestBase {

    @Test
    public void testVoid () throws IOException {
	parseAndSetClasses ("class Foo { void bar () {}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { void bar () { return; }}");
	assertNoErrors ();
    }

    @Test
    public void testVoidIntReturn () throws IOException {
	parseAndSetClasses ("class Foo { void bar () { return 3; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testInt () throws IOException {
	parseAndSetClasses ("class Foo { int bar () { return 3; }}");
	assertNoErrors ();
    }

    @Test
    public void testReturnUnreachableCode () throws IOException {
	parseAndSetClasses ("class Foo { int a; int bar () { return 3; a = 2; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testThrowUnreachableCode () throws IOException {
	parseAndSetClasses ("class Foo { int a; int bar () { throw new RuntimeException(); a = 2; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testIntMissingReturn () throws IOException {
	parseAndSetClasses ("class Foo { int bar () {}}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testIntVoidReturn () throws IOException {
	parseAndSetClasses ("class Foo { int bar () { return; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testIntStringReturn () throws IOException {
	parseAndSetClasses ("class Foo { int bar () { return \"bad\"; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testStringIntReturn () throws IOException {
	parseAndSetClasses ("class Foo { String bar () { return 4; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testStringReturn () throws IOException {
	parseAndSetClasses ("class Foo { String bar () { return \"good\"; }}");
	assertNoErrors ();
    }

    @Test
    public void testIntVariableReturn () throws IOException {
	parseAndSetClasses ("class Foo { int bar () { int i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int i = 3; int bar () { return i; }}");
	assertNoErrors ();
    }

    @Test
    public void testUpcastVariableReturn () throws IOException {
	parseAndSetClasses ("class Foo { short bar () { byte i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int bar () { byte i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { long bar () { byte i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int bar () { short i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { long bar () { short i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int bar () { char i = 0; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int bar () { char i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int bar () { char i = 65535; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { long bar () { char i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { long bar () { int i = 3; return i; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { double bar () { float i = 3; return i; }}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { int bar () { char i = 65536; return i; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testDowncast () throws IOException {
	parseAndSetClasses ("class Foo { float bar () { double d = 3.0; return d; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { float bar () { double d = 3.0; return (float)d; }}");
	assertNoErrors ();
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { int bar () { long l = 3; return l; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { int bar () { long l = 3; return (int)l; }}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { int bar () { int l = 3; return (int)l; }}");
	assertNoErrors ();
	assert diagnostics.hasWarning () : "Expected to find useless cast warning";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { Object bar () { String s = \"\"; return s; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { Object bar () { String s = \"\"; return (Object)s; }}");
	assertNoErrors ();
	assert diagnostics.hasWarning () : "Expected to find useless cast warning";
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSuperClassReturn () throws IOException {
	parseAndSetClasses ("class Foo { Object bar () { String s = \"hello\"; return s; }}");
	assertNoErrors ();
    }

    @Test
    public void testImplementedInterfaceReturn () throws IOException {
	parseAndSetClasses ("interface Bar {}",
			    "class Foo implements Bar { Bar bar () { Foo f = null; return f; }}");
	assertNoErrors ();
    }

    @Test
    public void testIfReturn () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    if (true)\n" +
			    "        return 2;\n" +
			    "    else\n" +
			    "        return 3;\n" +
			    "}}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    if (true)\n" +
			    "        return 2.0;\n" +
			    "    else\n" +
			    "        return 3;\n" +
			    "}}");
	assert diagnostics.hasError () : "Expected to find wrong return type";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    if (true)\n" +
			    "        return 2;\n" +
			    "    else\n" +
			    "        return 3.0;\n" +
			    "}}");
	assert diagnostics.hasError () : "Expected to find wrong return type";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    if (true)\n" +
			    "        return 2.0;\n" +
			    "}}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo {\n" +
			    "void bar (int a) {\n" +
			    "    int b = 0;\n" +
			    "    if (a > 3) {\n" +
			    "         b = 2;\n" +
			    "    } else {\n" +
			    "         b = 7;\n" +
			    "    }\n" +
			    "    b *= 3;\n" +
			    "}}");
	assertNoErrors ();
    }

    @Test
    public void testSwitchReturn () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int a;\n" +
			    "int bar (int i) {\n" +
			    "    switch (i) {\n" +
			    "    case 1: a++; return 2;\n" +
			    "    case 2: a--; return 3;\n" +
			    "    default: return 4;\n" +
			    "    }}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int a;\n" +
			    "int bar (int i) {\n" +
			    "    switch (i) {\n" +
			    "    case 1:\n" +
			    "    case 2:\n" +
			    "    default: return 4;\n" +
			    "    }}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int a;\n" +
			    "int bar (int i) {\n" +
			    "    switch (i) {\n" +
			    "    case 1:\n" +
			    "        a++;\n" +
			    "    case 2:\n" +
			    "    default: return 4;\n" +
			    "    }}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar (int i) {\n" +
			    "    switch (i) {\n" +
			    "    case 1: return 2;\n" +
			    "    case 2: return 3;\n" +
			    "    }}}");
	assert diagnostics.hasError () : "Expected missing return";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar (int i) {\n" +
			    "    switch (i) {\n" +
			    "    case 2: return 3.0;\n" +
			    "    }}}");
	assert diagnostics.hasError () : "Expected wrong type";
    }

    @Test
    public void testLabeledReturn () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    foo: return 17;\n" +
			    "    }}");
	assertNoErrors ();
    }

    @Test
    public void testBlockReturn () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    { return 17; }\n" +
			    "    }}");
	assertNoErrors ();
    }

    @Test
    public void testSynchronizedReturn () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    synchronized (this) { return 17; }\n" +
			    "    }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    synchronized (this) { return 17; }\n" +
			    "    return 3;\n" +
			    "    }}");
	assert diagnostics.hasError () : "Expected unreachable code";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    synchronized (this) { int a = 3; }\n" +
			    "    return 4;\n" +
			    "    }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    synchronized (this) { int a = 3; }\n" +
			    "    }}");
	assert diagnostics.hasError () : "Expected missing return";
    }

    @Test
    public void testWhile () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    while (true){return 3;}" +
			    "}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    while (true){}" +
			    "}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar (int a) {\n" +
			    "    while (a > 3 || true){return 3;}" +
			    "}}");
	assert diagnostics.hasError () : "Expected missing return";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    while (false){return 3;}\n" +
			    "    return 4;\n" +
			    "}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    while (false){return 3.0;}\n" +
			    "    return 4;\n" +
			    "}}");
	assert diagnostics.hasError () : "Expected wrong type";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    int i = 0;\n" +
			    "    while (true){if (i > 3) break; i++; }\n" +
			    "    return 4;\n" +
			    "}}");
	assertNoErrors ();
    }

    @Test
    public void testDo () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar (int i) {\n" +
			    "    do {return 3;} while (true);\n" +
			    "}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar (int i) {\n" +
			    "    do {return 3;} while (i > 3);\n" +
			    "}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar (int i) {\n" +
			    "    do {return 3;} while (i > 3); return 7;\n" +
			    "}}");
	assert diagnostics.hasError () : "Expected unreachable code";
    }

    @Test
    public void testFor () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {for (;;){return 3;}}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {for (;true;){return 3;}}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () { for (int i = 0;; i++){return 3;}}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () { for (int i = 0;; i++){ if (i > 5) break;} return 89;}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () { for (int i = 0; i < 3; i++){return 3;}}}");
	assert diagnostics.hasError () : "Expected missing return";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () { for (int i = 0; i < 3; i++){return 3;} return 7;}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () { for (;;) {for (int i = 0;; i++){ if (i > 5) break;}}}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () { for (;;) {for (int i = 0;; i++){ if (i > 5) break;}} return 77;}}");
	assert diagnostics.hasError () : "Expected unreachable code";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {int i = 0; for (;;) { break; i += 10;} return 3;}}");
	assert diagnostics.hasError () : "Expected unreachable code";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "outer: for (;;) {for (int i = 0;; i++){ if (i > 5) break outer; }} return 77;" +
			    "}}");
	assertNoErrors ();
    }

    @Test
    public void testBreak () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {foo: for (;;){break foo;} return 45;}}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo {\n" +
			    "void bar () {for (;;){break foo;}}}");
	assert diagnostics.hasError () : "Expected unknown label";
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testThrow () throws IOException {
	parseAndSetClasses ("class Foo {int bar () {throw new RuntimeException ();}}");
	assertNoErrors ();
    }

    @Test
    public void testTry () throws IOException {
	parseAndSetClasses ("class Foo {int bar () {try {return 3;} finally {}}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {int bar () {try {} finally {} return 3;}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {int bar () {try {return 72;} finally {return 3;}}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {int bar () {try {int a = 34;} finally {} return 47;}}");
	assertNoErrors ();
    }

    @Test
    public void testInnerClass () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    Runnable r = new Runnable () {\n" +
			    "        public void run () {\n" +
			    "            return;\n" +
			    "        }};\n" +
			    "    return 3;\n" +
			    "}}");
	assertNoErrors ();
    }

    @Test
    public void testIntVariableBadReturn () throws IOException {
 	parseAndSetClasses ("class Foo { String bar () {int i = 3; return i; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testNullReturn () throws IOException {
 	parseAndSetClasses ("class Foo { String bar () { return null; }}");
	assertNoErrors ();

 	parseAndSetClasses ("class Foo { int bar () { return null; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testExternalMethodReturn () throws IOException {
 	parseAndSetClasses ("class Foo { int bar () { return Integer.parseInt (\"3\"); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class Foo { int bar () { return Integer.parseInt (\"3\", 16); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class Foo { short s; int bar () { return Integer.hashCode(s); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class Foo { Object bar () { return getClass (); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class Foo { static Foo a; static int bar () { return System.identityHashCode (a); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class Foo { int bar () { return Integer.thisMethodShouldNotExist(); }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testInternalMethodReturn () throws IOException {
 	parseAndSetClasses ("class Foo { int foo () { return 3; } int bar () { return foo (); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class Foo { int foo (int i) { return 3; } int bar () { return foo (3); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class Foo { int foo (Object o) { return 3; } int bar () { return foo (\"foo\"); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class Foo {\n" +
			    "  byte foo (int i, long l) { return (byte)3; }\n" +
			    "  int bar () { return foo (3, 4); }}");
	assertNoErrors ();
    }

    @Test
    public void testChainedInternalMethodReturn () throws IOException {
 	parseAndSetClasses ("class P { Q q () { return new Q ();}}\n" +
			    "class Q { int i () { return 3; }}\n" +
			    "class R { int bar () { P p = new P (); return p.q ().i (); }}");
	assertNoErrors ();
    }

    @Test
    public void testExternalFieldReturn () throws IOException {
 	parseAndSetClasses ("import java.awt.Point;\n" +
			    "class Foo { int bar () { Point p = new Point(3, 7); return p.x; }}");
	assertNoErrors ();
    }

    @Test
    public void testInternalFieldReturn () throws IOException {
 	parseAndSetClasses ("class P { public int x; }\n" +
			    "class Q { int bar () { P p = new P (); return p.x; }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { public String x; }\n" +
			    "class Q { String bar () { P p = new P (); return p.x; }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { public String x; }\n" +
			    "class Q { Object bar () { P p = new P (); return p.x; }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { public long x; }\n" +
			    "class Q { int bar () { P p = new P (); return p.x; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testChainedInternalFieldReturn () throws IOException {
 	parseAndSetClasses ("class P { public Q q; }\n" +
			    "class Q { public int i; }\n" +
			    "class R { int bar () { P p = new P (); return p.q.i; }}");
	assertNoErrors ();
    }

    @Test
    public void testReturnNew () throws IOException {
 	parseAndSetClasses ("class P { }\n" +
			    "class Q { P p () { return new P (); }}");
	assertNoErrors ();

 	parseAndSetClasses ("class P { }\n" +
			    "class Q { P p () { return new P () {}; }}");
	assertNoErrors ();

 	parseAndSetClasses ("interface P { }\n" +
			    "class Q { P p () { return new P () {}; }}");
	assertNoErrors ();
    }

    @Test
    public void testReturnEnum () throws IOException {
 	parseAndSetClasses ("enum E { A, B, C }\n" +
			    "class Q { E e () { return E.A; }}");
	assertNoErrors ();
    }

    @Test
    public void testNameClashWithPrimitive () throws IOException {
 	parseAndSetClasses ("class I {}\n" +
			    "class Foo { int bar () { I i; return i; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testIfExpression () throws IOException {
 	parseAndSetClasses ("class P { void m () { if (true) { return; }}}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { void m () { if (false) { return; }}}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { void m () { if (12) { return; }}}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { void m (String s) { if (s) { return; }}}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testUnaryExpression () throws IOException {
 	parseAndSetClasses ("class P { int m (int i) { return ~i; }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { double m (double i) { return ~i; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { boolean m (boolean b) { return !b; }}");
	assertNoErrors ();
    }

    @Test
    public void testTwoPartAutoExtend () throws IOException {
 	parseAndSetClasses ("class P { int m () { return 3 + 4L; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { int m () { return 3L + 4; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testTwoPartNull () throws IOException {
 	parseAndSetClasses ("class P { int m () { return 3 + null; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { int m () { return null + 3; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { boolean m (Object o) { return null == o; }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { boolean m (Object o) { return o == null; }}");
	assertNoErrors ();
    }

    @Test
    public void testTwoPartBitHandling () throws IOException {
 	parseAndSetClasses ("class P { int m () { float f = 3f & 7; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { int m () { float f = 7 | 3f; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { int m () { float f = 7 ^ 3f; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { int m () { int i = 7 << 3f; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { int m () { int i = 7 >> 1f; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();
 	parseAndSetClasses ("class P { int m () { int i = 7d >>> 1f; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testIncrementDecrement () throws IOException {
 	parseAndSetClasses ("class P { int ii (int i) { return i++; }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { int ii (int i) { return ++i; }}");
	assertNoErrors ();

 	parseAndSetClasses ("class P { float ii (float i) { return i++; }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { float ii (float i) { return ++i; }}");
	assertNoErrors ();

 	parseAndSetClasses ("class P { boolean bi (boolean b) { return ++b; }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

 	parseAndSetClasses ("class P { boolean bi (boolean b) { return b++; }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testConstructors () throws IOException {
 	parseAndSetClasses ("class P { public P p () { return new P (); }}");
	assertNoErrors ();

 	parseAndSetClasses ("class P { public P () {} public P p (int i) { return new P (3.0); }}");
	assert diagnostics.hasError () : "Expected to find errors";
	diagnostics = new CompilerDiagnosticCollector ();

 	parseAndSetClasses ("class P { public P () {} public P p () { return new P (); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { public P (int i) {} public P p () { return new P (4711); }}");
	assertNoErrors ();
 	parseAndSetClasses ("class P { public P (long l) {} public P p () { return new P (4711); }}");
	assertNoErrors ();
    }

    @Test
    public void testThis () throws IOException {
	parseAndSetClasses ("class A { public A (int i) { this.j = i; }}");
	assert diagnostics.hasError () : "There is no j";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class A { public A (int i) { this.i = i; }}");
	assert diagnostics.hasError () : "There is no this.i";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class A { int i; public A (int i) { this.i = i; }}");
	assertNoErrors ();
    }

    @Test
    public void testTernery () throws IOException {
	parseAndSetClasses ("class A { int a (boolean b) { return b ? 1 : 2; }}");
	assertNoErrors ();
	parseAndSetClasses ("class A { String a (boolean b) { return b ? \"\" : \"a\"; }}");
	assertNoErrors ();
	parseAndSetClasses ("class A { long a (boolean b) { byte s = 3; return b ? s : 12L; }}");
	assertNoErrors ();
    }

    @Test
    public void testUninitializedLocalVariable () throws IOException {
	parseAndSetClasses ("class A { int a () { int a = 10; return a; }}");
	assertNoErrors ();

	parseAndSetClasses ("class A { int a () { int a; return a; }}");
	assert diagnostics.hasError () : "Expected uninitialized variable";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class A { int a () { int a; a = 10; return a; }}");
	assertNoErrors ();

	parseAndSetClasses ("class A { int a () { int a, b, c; a = b = c = 10; return a * b * c; }}");
	assertNoErrors ();

	parseAndSetClasses ("class A { int a () { int a; if (1 > 2) a = 10; return a; }}");
	assert diagnostics.hasError () : "Expected uninitialized variable";
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class A { int a () { int a; if (1 > 2) { a = 10; } else { a = 7; } return a; }}");
	assertNoErrors ();

	parseAndSetClasses ("class A { int a () { int a, b; a = 1 > 2 ? (b = 10) : (b = 3); return a * b; }}");
	assertNoErrors ();

	parseAndSetClasses ("class A { int a () { int a, b; a = 1 > 2 ? (b = 10) : 3 ; return a * b; }}");
	assert diagnostics.hasError () : "Expected uninitialized variable";
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testMethodOnGenericType () throws IOException {
	parseAndSetClasses ("class Foo<T> { T t; String foo () { return t.toString (); }}");
	assertNoErrors ();
    }

    @Test
    public void testMethodOnGenericExtendedType () throws IOException {
	parseAndSetClasses ("class Foo<T extends Runnable> { T t; void foo () { t.run (); }}");
	assertNoErrors ();
    }

    @Test
    public void testAccessToOuterField () throws IOException {
	parseAndSetClasses ("class Foo {Object t; public class B {  public B () { t = new Object ();}}}");
	assertNoErrors ();
    }

    @Test
    public void testAccessToOuterStaticField () throws IOException {
	parseAndSetClasses ("class Foo {static Object t; public class B {  public B () { t = new Object ();}}}");
	assertNoErrors ();
    }

    @Test
    public void testStaticAccessToOuterStaticField () throws IOException {
	parseAndSetClasses ("class Foo {static Object t; public static class B {  public B () { t = new Object ();}}}");
	assertNoErrors ();
    }

    @Test
    public void testConstructorWithoutArgs () throws IOException {
	parseAndSetClasses ("import java.util.HashMap; import java.util.Map; class Foo {Map<String, Foo> nameToToken = new HashMap<> ();}");
	assertNoErrors ();
    }

    @Test
    public void testVarargCallNoActualArguments () throws IOException {
	parseAndSetClasses ("class Foo { void f (Object...os) {} void g () {f();}}");
	assertNoErrors ();
    }

    @Test
    public void testVarargCallOneActualArgument () throws IOException {
	parseAndSetClasses ("class Foo { void f (Object...os) {} void g () {f(\"Hello World!\");}}");
	assertNoErrors ();
    }

    @Test
    public void testVarargCallTwoActualArguments () throws IOException {
	parseAndSetClasses ("class Foo { void f (Object...os) {} void g () {f(\"Hello\", \" World!\");}}");
	assertNoErrors ();
    }

    @Test
    public void testVarargRTClass () throws IOException {
	parseAndSetClasses ("import java.util.EnumSet; enum Foo { A; EnumSet<Foo> foos = EnumSet.of (A, A, A, A, A, A); }");
	assertNoErrors ();
    }

    @Test
    public void testMultipleMethodOptions () throws IOException {
	parseAndSetClasses ("class Foo { void f (Object o, Integer i) {} void f (Integer i, Object o) {}" +
			    " void h () { Integer i = Integer.valueOf (1); f (i, i); }}");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testOverridenMethod () throws IOException {
	parseAndSetClasses ("class Foo { void f (Integer i) {}} " +
			    "class Bar extends Foo { void f (Integer i) {} " +
			    " void h () { Integer i = Integer.valueOf (1); f (i); }}");
	assertNoErrors ();
    }

    @Test
    public void testMultipleOptionsWithExactMatch () throws IOException {
	parseAndSetClasses ("class Foo { void f (char c) {} void f (int i) {} void g () { char c = 0; f(c); }}");
	assertNoErrors ();
    }

    @Test
    public void testMultipleOptionsWithBetterMatch () throws IOException {
	parseAndSetClasses ("class Foo { void f (int i) {} void f (long l) {} void g () { char c = 0; f(c); }}");
	assertNoErrors ();
    }

    @Test
    public void testMultipleMatchAutobox () throws IOException {
	parseAndSetClasses ("class Foo { void f (int i) {} void f (Integer i) {} void g () { int i = 0; f (i); }}");
	assertNoErrors ();
    }

    @Test
    public void testMultipleMatchNormalWinsOverVarargs () throws IOException {
	parseAndSetClasses ("class Foo { void f (Object o) {} void f (Object... os) {} void g () { Object o = null; f (o); }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { void f (Object o, Object p) {} void f (Object... os) {} void g () { Object o = null; f (o, o); }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { void f (Object o) {} void f (Object... os) {} void g () { f (); }}");
	assertNoErrors ();
    }

    @Test
    public void testMultipleMatchVarargsSpecialization () throws IOException {
	// Should match String...
	parseAndSetClasses ("class Foo { void f (Object...os) {} void f (String...ss) {} void g () { String s = \"asdf\"; f (s, s, s); }}");
	assertNoErrors ();
	// Should match Object...
	parseAndSetClasses ("class Foo { void f (Object...os) {} void f (String...ss) {} " +
			    " void g () { String s = \"asdf\"; Integer i = Integer.valueOf (1); f (s, s, i); }}");
	assertNoErrors ();
    }

    protected void handleSyntaxTree (SyntaxTree tree) {
	FieldAndMethodSetter mis = new FieldAndMethodSetter (cip, tree, diagnostics);
	mis.run ();
	ReturnChecker rc = new ReturnChecker (cip, tree, diagnostics);
	rc.run ();
    }
}