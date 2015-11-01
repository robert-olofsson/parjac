package org.khelekore.parjac.semantics;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.tree.SyntaxTree;
import org.testng.annotations.Test;

public class TestReturnChecker extends TestBase {

    @Test
    public void testVoid () throws IOException {
	parseAndSetClasses ("class Foo { void bar () {} }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { void bar () { return; } }");
	assertNoErrors ();
    }

    @Test
    public void testVoidIntReturn () throws IOException {
	parseAndSetClasses ("class Foo { void bar () { return 3; } }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testInt () throws IOException {
	parseAndSetClasses ("class Foo { int bar () { return 3; } }");
	assertNoErrors ();
    }

    @Test
    public void testReturnUnreachableCode () throws IOException {
	parseAndSetClasses ("class Foo { int a; int bar () { return 3; a = 2; } }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testThrowUnreachableCode () throws IOException {
	parseAndSetClasses ("class Foo { int a; int bar () { throw new RuntimeException(); a = 2; } }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testIntMissingReturn () throws IOException {
	parseAndSetClasses ("class Foo { int bar () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testIntVoidReturn () throws IOException {
	parseAndSetClasses ("class Foo { int bar () { return; } }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testIntStringReturn () throws IOException {
	parseAndSetClasses ("class Foo { int bar () { return \"bad\"; } }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testStringIntReturn () throws IOException {
	parseAndSetClasses ("class Foo { String bar () { return 4; } }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testStringReturn () throws IOException {
	parseAndSetClasses ("class Foo { String bar () { return \"good\"; } }");
	assertNoErrors ();
    }

    @Test
    public void testIntVariableReturn () throws IOException {
	parseAndSetClasses ("class Foo { int bar () { int i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int i = 3; int bar () { return i; } }");
	assertNoErrors ();
    }

    @Test
    public void testUpcastVariableReturn () throws IOException {
	parseAndSetClasses ("class Foo { short bar () { byte i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int bar () { byte i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { long bar () { byte i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int bar () { short i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { long bar () { short i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { int bar () { char i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { long bar () { char i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { long bar () { int i = 3; return i; } }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { double bar () { float i = 3; return i; } }");
	assertNoErrors ();
    }

    @Test
    public void testSuperClassReturn () throws IOException {
	parseAndSetClasses ("class Foo { Object bar () { String s = \"hello\"; return s; } }");
	assertNoErrors ();
    }

    @Test
    public void testImlementedInterfaceReturn () throws IOException {
	parseAndSetClasses ("interface Bar {}",
			    "class Foo implements Bar { Bar bar () { Foo f = null; return f; } }");
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
	assert diagnostics.hasError () : "Expected missing return";
    }

    @Test
    public void testFor () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    for (; true; ){return 3;}" +
			    "}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    for (int i = 0; i < 3; i++){return 3;}" +
			    "}}");
	assert diagnostics.hasError () : "Expected missing return";
	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    for (int i = 0; i < 3; i++){return 3;}" +
			    "    return 3234;\n" +
			    "}}");
	assertNoErrors ();
    }

    @Test
    public void testThrow () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    throw new RuntimeException ();\n" +
			    "    }}");
	assertNoErrors ();
    }

    @Test
    public void testTry () throws IOException {
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    try {\n" +
			    "        return 3;" +
			    "    } finally {}\n" +
			    "    }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    try {\n" +
			    "    } finally {}\n" +
			    "        return 3;" +
			    "    }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    try {\n" +
			    "        return 72;\n" +
			    "    } finally {\n" +
			    "        return 3;" +
			    "    }}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo {\n" +
			    "int bar () {\n" +
			    "    try {\n" +
			    "        int a = 34;\n" +
			    "    } finally {\n" +
			    "    }\n" +
			    "    return 47;\n" +
			    "}}");
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
 	parseAndSetClasses ("class Foo { String bar () {int i = 3; return i; } }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testNullReturn () throws IOException {
 	parseAndSetClasses ("class Foo { String bar () { return null; } }");
	assertNoErrors ();

 	parseAndSetClasses ("class Foo { int bar () { return null; } }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    protected void handleSyntaxTree (SyntaxTree tree) {
	ReturnChecker rc = new ReturnChecker (cip, tree, diagnostics);
	rc.run ();
    }
}