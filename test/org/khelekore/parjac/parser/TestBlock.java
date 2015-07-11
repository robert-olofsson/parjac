package org.khelekore.parjac.parser;

import java.io.IOException;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestBlock {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () throws IOException {
	g = TestParseHelper.getJavaGrammarFromFile ("Block", false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testEmptyBlock () {
	testSuccessfulParse ("{ }");
	testSuccessfulParse ("{ ; }");
	testSuccessfulParse ("{ ;;; }");
    }

    @Test
    public void testSimpleBlock () {
	testSuccessfulParse ("{ int a = b + c; }");
    }

    @Test
    public void testConditionalExpressions () {
	testSuccessfulParse ("{ int a = b * c; }");
	testSuccessfulParse ("{ int a = b / c; }");
	testSuccessfulParse ("{ int a = b + c; }");
	testSuccessfulParse ("{ int a = b - c; }");
	testSuccessfulParse ("{ int a = b & c; }");
	testSuccessfulParse ("{ int a = b | c; }");
	testSuccessfulParse ("{ int a = b ^ c; }");
	testSuccessfulParse ("{ int a = b % c; }");
	testSuccessfulParse ("{ boolean a = b == c; }");
	testSuccessfulParse ("{ boolean a = b != c; }");
	testSuccessfulParse ("{ int a = b << 1; }");
	testSuccessfulParse ("{ int a = b << c; }");
	testSuccessfulParse ("{ int a = b >> 1; }");
	testSuccessfulParse ("{ int a = b >> c; }");
	testSuccessfulParse ("{ boolean a = b > c; }");
	testSuccessfulParse ("{ boolean a = b < c; }");
	testSuccessfulParse ("{ boolean a = a.b < c; }");
	testSuccessfulParse ("{ boolean a = a.b.c < d; }");
	testSuccessfulParse ("{ a = b < c; }");
	testSuccessfulParse ("{ a = b + c; }");
	testSuccessfulParse ("{ a = b == c; }");
	testSuccessfulParse ("{ a = (b == null); }");
	testSuccessfulParse ("{ a = null == b; }");
	testSuccessfulParse ("{ a = b instanceof Foo; }");
    }

    @Test
    public void testMethodCall () {
	testSuccessfulParse ("{ foo(); }");
	testSuccessfulParse ("{ int a = foo (); }");
	testSuccessfulParse ("{ a.foo (); }");
	testSuccessfulParse ("{ a.b.foo (); }");
	testSuccessfulParse ("{ super.foo (); }");
	testSuccessfulParse ("{ a.super.foo (); }");
    }

    @Test
    public void testFor () {
	testSuccessfulParse ("{ for (Foo foo : listOfFoo) {}\n}");
	testSuccessfulParse ("{ for (int i = CONSTANT; i >= 0; i--) {}}");
	testSuccessfulParse ("{ for (int i = 0; i < CONSTANT; i++) {}}");
	testSuccessfulParse ("{ for (int i = 0; i < 10; i++) {}}");
	testSuccessfulParse ("{ for (int i = 0, j = 0, k = a; bar1.b; i++) {}}");
	testSuccessfulParse ("{ for ( ; true; ) {}}");
	testSuccessfulParse ("{ for ( ; ; ) {}}");
    }

    @Test
    public void testIf () {
	testSuccessfulParse ("{ if (true) a = b; }");
	testSuccessfulParse ("{ if (true) return a; }");
	testSuccessfulParse ("{ if (true) return a; else return b; }");
	testSuccessfulParse ("{ if (true) { return a; } else { return b; }}");
    }

    @Test
    public void testWhile () {
	testSuccessfulParse ("{ while (true) { i++; }}");
	testSuccessfulParse ("{ do { i++; } while (true); }");
	testSuccessfulParse ("{ while (i < 4) { i++; }}");
	testSuccessfulParse ("{ while (i < foo) { i++; }}");
    }

    @Test
    public void testSwitch () {
	testSuccessfulParse ("{ switch (foo) {\n" +
			     "default: a = b; }}");
	testSuccessfulParse ("{ switch (foo) {\n" +
			     "case 1: a = foo; break;\n" +
			     "case 2: a = bar; break;\n" +
			     "case 3: a = baz; break;\n" +
			     "}}");
	testSuccessfulParse ("{ switch (foo) {\n" +
			     "case 1: a = b; break;\n" +
			     "case 2: b = 2; break;\n" +
			     "default: c = 3; }}");
	testSuccessfulParse ("{ switch (foo) {\n" +
			     "case A: return 1;\n" +
			     "case B: case C: return 2;\n" +
			     "default: return 3;\n" +
			     "}}");
    }

    @Test
    public void testReturn () {
	testSuccessfulParse ("{return true;}");
	testSuccessfulParse ("{return 1;}");
	testSuccessfulParse ("{return 1.0;}");
	testSuccessfulParse ("{return a || b;}");
	testSuccessfulParse ("{return 2 * 3;}");
	testSuccessfulParse ("{return 2 + 3;}");
	testSuccessfulParse ("{return 2 & 3;}");
    }

    @Test
    public void testLocalVariableDeclaration () {
	testSuccessfulParse ("{int i;}");
	testSuccessfulParse ("{int i, j;}");
	testSuccessfulParse ("{int[] i, j;}");
	testSuccessfulParse ("{int[] i[], j[][];}");
	testSuccessfulParse ("{int i = 1;}");
	testSuccessfulParse ("{int i = a;}");
	testSuccessfulParse ("{int i = 1 + 2;}");
	testSuccessfulParse ("{int i = 1 + a;}");
	testSuccessfulParse ("{int i;\nFoo foo;\nBar baz;\ndouble d; }");
	testSuccessfulParse ("{foo.Bar bar; }");
	testSuccessfulParse ("{foo.bar.Baz baz; }");
	testSuccessfulParse ("{foo.Bar bar = new foo.Bar(); }");
	testSuccessfulParse ("{foo.bar.Baz baz = new foo.bar.Baz(); }");
	testSuccessfulParse ("{Map<D, S> work = new TreeMap<D, S> (); }");
	testSuccessfulParse ("{Class<Boolean> c = boolean.class; }");
	testSuccessfulParse ("{Class<Integer> c = int.class; }");
	testSuccessfulParse ("{Foo foo = null, bar = null;}");
    }

    @Test
    public void testNew () {
	testSuccessfulParse ("{Foo foo = new Foo ();}");
	testSuccessfulParse ("{Bar bar = new foo.Bar ();}");
	testSuccessfulParse ("{Baz baz = new foo.bar.Baz ();}");
	testSuccessfulParse ("{rules = new ArrayList<> ();}");
	testSuccessfulParse ("{return new <T> @Foo Foo.Bar<> (1, 2, 3);}");
	testSuccessfulParse ("{return new <T> @Foo Foo.@Bar Bar<> (1, 2, 3);}");
	testSuccessfulParse ("{return new <T> @Foo Foo.Bar<> (1, 2, 3) {};}");
	testSuccessfulParse ("{return a.new Foo ();}");
    }

    @Test
    public void testSynchronized () {
	testSuccessfulParse ("{synchronized (foo) { i = 0; }}");
    }

    @Test
    public void testThrow () {
	testSuccessfulParse ("{throw e;}");
	testSuccessfulParse ("{throw new FooException(\"blargle\");}");
    }

    @Test
    public void testTry () {
	testSuccessfulParse ("{try { a = b; } catch (A ex) {}}");
	testSuccessfulParse ("{try { a = b; } finally { c = d; }}");
	testSuccessfulParse ("{try { a = b; } catch (AException a) { } finally { c = d; }}");
	testSuccessfulParse ("{try { a = b; } catch (A | B ex) { } finally { c = d; }}");
	testSuccessfulParse ("{try { a = b; } catch (A | B | C ex) { } finally { c = d; }}");

	testSuccessfulParse ("{try (Foo foo = new Foo()) { a = b; } }");
	testSuccessfulParse ("{try (Foo foo = new Foo(); Bar bar = new Bar(foo)) { a = b; } }");
    }

    @Test
    public void testAssert () {
	testSuccessfulParse ("{ assert foo; }");
	testSuccessfulParse ("{ assert foo : \"lorem...\"; }");
    }

    @Test
    public void testLambda () {
	testSuccessfulParse("{ btn.foo(e -> System.out.println(\"Hello World!\")); }");
	testSuccessfulParse("{ btn.foo((k, v) -> k - v); }");
	testSuccessfulParse("{ IntegerMath addition = (a, b) -> a + b; }");
	testSuccessfulParse("{ String s = invoke(() -> \"done\"); }");
	testSuccessfulParse("{ btn.foo((int i) -> i + 2); }");
	testSuccessfulParse("{ btn.foo((int i, int j) -> i + j); }");
	testSuccessfulParse("{ Consumer<Integer>  c = (int x) -> { System.out.println(x); }; }");
    }

    @Test
    public void testCast () {
	testSuccessfulParse ("{ int i = (int)j; }");
	testSuccessfulParse ("{ Foo foo = (Foo)bar; }");
	testSuccessfulParse ("{ foo = (foo.Bar)bar; }");
	testSuccessfulParse ("{ Baz foo = (foo.bar.Baz)bar; }");
	testSuccessfulParse ("{ foo = (Foo & bar.Baz)bar; }");
	testSuccessfulParse ("{ foo = (foo.Bar & Baz)bar; }");
	testSuccessfulParse ("{ foo = (foo.Bar)bar; }");
	testSuccessfulParse ("{ Foo<T> foo = (Foo<T>)bar; }");
	testSuccessfulParse ("{ Bar<T> foo = (foo.Bar<T>)bar; }");
	testSuccessfulParse ("{ Baz<T> foo = (foo.bar.Baz<T>)bar; }");
	testSuccessfulParse ("{ foo = (Foo<T> & Bar<T>)bar; }");
	testSuccessfulParse ("{ foo = (@Foo Bla)bar; }");
	testSuccessfulParse ("{ foo = (@Foo Bla<T>)bar; }");
	testSuccessfulParse ("{ foo = (int[])bar; }");
	testSuccessfulParse ("{ foo = (@Foo int[])bar; }");
	testSuccessfulParse ("{ foo = (int[][])bar; }");
	testSuccessfulParse ("{ foo = (Foo[])bar; }");
	testSuccessfulParse ("{ foo = (foo.Bar[])bar; }");
	testSuccessfulParse ("{ foo = (@Foo Bla<T>.bleh<S>)bar; }");
	testSuccessfulParse ("{ foo = (@Foo Bla<T>.@Bar bleh<S>)bar; }");
	testSuccessfulParse ("{ NCacheEntry<K, V> nent = (NCacheEntry<K, V>)ent; }");
    }

    @Test
    public void testFieldAccess () {
	testSuccessfulParse ("{ return a.b; }");
	testSuccessfulParse ("{ return super.b; }");
	testSuccessfulParse ("{ return a.super.b; }");
    }

    @Test
    public void testArrayAccess () {
	testSuccessfulParse ("{ return a[0]; }");
	testSuccessfulParse ("{ return a[b]; }");
	testSuccessfulParse ("{ return a.b[0]; }");
    }

    @Test
    public void testArrayCreationExpression () {
	testSuccessfulParse ("{ return new int[3]; }");
	testSuccessfulParse ("{ return new int[3][4]; }");
	testSuccessfulParse ("{ return new int[a][4]; }");
	testSuccessfulParse ("{ return new int[a][b]; }");
	testSuccessfulParse ("{ return new int[a][]; }");
	testSuccessfulParse ("{ return new int[] {}; }");
	testSuccessfulParse ("{ return new int[] {1}; }");
	testSuccessfulParse ("{ return new int[] {1, 2}; }");
    }

    @Test
    public void testMethodReference () {
	testSuccessfulParse ("{ return Foo::bar; }");
	testSuccessfulParse ("{ return Foo::<Bar>bar; }");
	testSuccessfulParse ("{ return super::bar; }");
	testSuccessfulParse ("{ return super::<Bar>bar; }");
	testSuccessfulParse ("{ return a.super::<Bar>bar; }");
	testSuccessfulParse ("{ return Foo::new; }");
	testSuccessfulParse ("{ return Foo::<Bar>new; }");
	testSuccessfulParse ("{ return Foo[]::new; }");
	testSuccessfulParse ("{ return int[]::new; }");
    }

    @Test
    public void testPrimaryNoNewArray () {
	testSuccessfulParse ("{ return A.class; }");
	testSuccessfulParse ("{ return A[].class; }");
	testSuccessfulParse ("{ return A[][].class; }");
	testSuccessfulParse ("{ return A[][][].class; }");
	testSuccessfulParse ("{ return int.class; }");
	testSuccessfulParse ("{ return int[].class; }");
	testSuccessfulParse ("{ return void.class; }");
	testSuccessfulParse ("{ return this; }");
	testSuccessfulParse ("{ return (this); }");
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
}