package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.tree.AdditionalBound;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExtendsInterfaces;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.InterfaceTypeList;
import org.khelekore.parjac.tree.MethodDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SimpleClassType;
import org.khelekore.parjac.tree.TypeArguments;
import org.khelekore.parjac.tree.TypeBound;
import org.khelekore.parjac.tree.TypeParameter;
import org.khelekore.parjac.tree.TypeParameters;
import org.testng.annotations.Test;

public class TestClassSetter extends TestBase {

    @Test
    public void testExtendsNonExisting () throws IOException {
	parseAndSetClasses ("class Bar extends QWER {}");
	assertErrors ();
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
	assertErrors ();
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
    public void testMultiClassBadOrder () throws IOException {
	parseAndSetClasses ("class Bar extends Foo {} class Foo {}");
	assertNoErrors ();
    }

    @Test
    public void testMultiClassFieldBadOrder () throws IOException {
	parseAndSetClasses ("class Bar extends Baz { FooBar foo; }\n" +
			    "class Baz extends Foo {}\n" +
			    "class Foo { enum FooBar {}}");
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
    public void testInnerClassField () throws IOException {
	parseAndSetClasses ("package foo.bar; class Foo { class Bar {} Bar bar;}");
	assertNoErrors ();
	checkField ("foo.bar.Foo", 1, "foo.bar.Foo$Bar");
    }

    @Test
    public void testInnerClassLocal () throws IOException {
	parseAndSetClasses ("package foo.bar; class Foo { class Bar {} void f () { new Bar ();}}");
	assertNoErrors ();
    }

    @Test
    public void testEnumExtends () throws IOException {
	parseAndSetClasses ("package foo.bar;\n" +
			    "interface Foo {}\n" +
			    "enum E implements Foo {A, B}");
	assertNoErrors ();
	EnumDeclaration ed = (EnumDeclaration)cip.getType ("foo.bar.E");
	checkOneInterface (ed.getSuperInterfaces (), "foo.bar.Foo");
    }

    @Test
    public void testInterfaceExtends () throws IOException {
	parseAndSetClasses ("package foo.bar; interface Foo {} interface Bar extends Foo {}");
	assertNoErrors ();
	NormalInterfaceDeclaration id = (NormalInterfaceDeclaration)cip.getType ("foo.bar.Bar");
	ExtendsInterfaces ei = id.getExtendsInterfaces ();
	assert ei != null;
	checkOneInterface (ei.get (), "foo.bar.Foo");
    }

    @Test
    public void testInnerParamOuterEnum () throws IOException {
	parseAndSetClasses ("package foo; " +
			    "class Foo { enum Type {A, B} static class P { Type t; }}");
	assertNoErrors ();
    }

    @Test
    public void testGenericClassType () throws IOException {
	parseAndSetClasses ("package foo; class Foo<T> { T t; }");
	assertNoErrors ();
    }

    @Test
    public void testGenericSuperType () throws IOException {
	parseAndSetClasses ("package foo; class Foo implements Comparable<Foo> {}");
	NormalClassDeclaration cd = (NormalClassDeclaration)cip.getType ("foo.Foo");
	InterfaceTypeList ifs = cd.getSuperInterfaces ();
	assert ifs != null;
	List<ClassType> cts = ifs.get ();
	assert cts != null;
	assert cts.size () == 1;
	ClassType ct = cts.get (0);
	assert ct.getFullName ().equals ("java.lang.Comparable");
	SimpleClassType sct = ct.get ().get (0);
	TypeArguments tas = sct.getTypeArguments ();
	ClassType ctt = (ClassType)(tas.getTypeArguments ().get (0));
	assert ctt.getFullName ().equals ("foo.Foo");
	assertNoErrors ();
    }

    @Test
    public void testGenericClassTypeWithGenericSuperclass () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "class Foo<T> {}\n" +
			    "class Bar<T> extends Foo<T> {}");
	assertNoErrors ();
    }

    @Test
    public void testGenericMethodType () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "class Foo {\n" +
			    "    <T> T foo (T t) { T t2; }\n" +
			    "}");
	assertNoErrors ();
    }

    @Test
    public void testGenericTypeInConstructor () throws IOException {
	parseAndSetClasses ("class Foo<T> { private T t; public Foo (T t) {this.t = t;}}");
	assertNoErrors ();
    }

    @Test
    public void testImportedInner () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.Map.Entry;\n" +
			    "class Foo implements Entry {}");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassFromSuperClassCompiled () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "class Foo {\npublic class Bar {}}\n" +
			    "class Baz extends Foo { Bar doBar() {return null;} }");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassFromSuperInterfaceCompiled () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "interface Foo { enum Bar { A, B }}\n" +
			    "class Baz implements Foo { Bar doBar() {return null;} }");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassFromSuperSuperClassCompiled () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "class Foo {\npublic class Bar {}}\n" +
			    "class Baz extends Foo {}\n" +
			    "class Quop extends Baz { Bar doBar() {return null;} }");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassFromSuperClass () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.Base64;\n" +
			    "class Foo extends Base64 { Decoder d = null; }");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassFromSuperInterface () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.HashMap;\n" +
			    "class Foo extends HashMap { Entry e = null; }");
	assertNoErrors ();
    }

    @Test
    public void testConstructorWithType () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "class Foo { <T> Foo (T t) {} }");
	assertNoErrors ();
    }

    @Test
    public void testSingleStaticImport () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import static java.util.Map.Entry;\n" +
			    "class Foo {Entry e = null;}");
	assertNoErrors ();
    }

    @Test
    public void testTypeImportOnDemand () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.Base64.*;\n" +
			    "class Foo { Decoder d; }");
	assertNoErrors ();
    }

    @Test
    public void testStaticImportOnDemand () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import static java.util.concurrent.locks.ReentrantReadWriteLock.*;\n" +
			    "class Foo { ReadLock rl; WriteLock wl; }");
	assertNoErrors ();
    }

    @Test
    public void testPrimitiveArray () throws IOException {
	parseAndSetClasses ("class Foo { int[] ia; }");
	assertNoErrors ();
    }

    @Test
    public void testStringArray () throws IOException {
	parseAndSetClasses ("class Foo { String[] ia; }");
	assertNoErrors ();
    }

    @Test
    public void testSimpleWildCard () throws IOException {
	parseAndSetClasses ("class Foo { Class<?> c; }");
	assertNoErrors ();
    }

    @Test
    public void testGenericTypeExtends () throws IOException {
	parseAndSetClasses ("package foo; class Foo<T> { void foo (Foo<? extends Foo> e) {} }");
	assertNoErrors ();
    }

    @Test
    public void testGenericTypeSuper () throws IOException {
	parseAndSetClasses ("package foo; class Foo<T> { void foo (Foo<? super Foo> s) {} }");
	assertNoErrors ();
    }

    @Test
    public void testMultiGenericType () throws IOException {
	parseAndSetClasses ("package foo;" +
			    "import java.util.List; " +
			    "class Foo { List<List<List<String>>> ls; }");
	assertNoErrors ();
    }

    @Test
    public void testBootpathClass () throws IOException {
	// Cipher is in jce.jar, not in rt.jar
	parseAndSetClasses ("package foo;\n" +
			    "import javax.crypto.*;\n" +
			    "class Foo {\n" +
			    "    class Bar {\n" +
			    "        private Cipher cipher;\n" +
			    "    }\n" +
			    "}");
	assertNoErrors ();
    }

    @Test
    public void testSuperInnerFromAnonymous () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "class Foo { class Bar {} }\n" +
			    "class Baz {\n" +
			    "    Foo foo () {\n" +
			    "        return new Foo () { public Bar bar () { return null; }};\n" +
			    "    }\n" +
			    "}");
	assertNoErrors ();
    }

    @Test
    public void testSuperInnerFromAnonymousFQN () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "class Foo { class Bar {} }\n" +
			    "class Baz {\n" +
			    "    Foo foo () {\n" +
			    "        return new foo.Foo () { public Foo.Bar bar () { return null; }};\n" +
			    "    }\n" +
			    "}");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassAndAnonymousClass () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "public class GC {\n" +
			    "private void foo (Filter filter) {}\n" +
			    "private void bar () {\n" +
			    "new Thread (new Runnable () {public void run () {}});\n" +
			    "}\n" +
			    "public static class Filter {}\n" +
			    "}");
	assertNoErrors ();
    }

    @Test
    public void testFullName () throws IOException {
	parseAndSetClasses ("class Foo extends java.util.HashMap {}");
	assertNoErrors ();
	parseAndSetClasses ("package foo;\n" +
			    "class Foo {}\n" +
			    "class Bar extends foo.Foo {}");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassSubpackage () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "class RPG { public interface TG {} }\n" +
			    "class IPG extends RPG {}\n" +
			    "class STG1 implements IPG.TG {}\n" +
			    "class STG2 implements foo.IPG.TG {}");
	assertNoErrors ();
    }

    @Test
    public void testMultiType () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.Map;\n" +
			    "abstract class MyEntry implements Map.Entry {}\n");
	checkImplements ("foo.MyEntry", "java.util.Map$Entry");
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.HashMap;\n" +
			    "abstract class Foo implements HashMap.Entry {}\n");
	checkImplements ("foo.MyEntry", "java.util.Map$Entry");
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.HashMap;\n" +
			    "abstract class Foo implements java.util.HashMap.Entry {}\n");
	checkImplements ("foo.MyEntry", "java.util.Map$Entry");
	assertNoErrors ();
    }

    @Test
    public void testUnusedImports () throws IOException {
	parseAndSetClasses ("import java.util.Map;\n" +
			    "abstract class Foo {}\n");
	assert diagnostics.hasWarning () : "Expected unused import warnings";
    }

    @Test
    public void testSingleTypeImportNameClash () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import java.awt.List;\n" +
			    "import java.util.List;\n" +
			    "class Qwer { List l; }\n");
	assert diagnostics.hasError () : "Expected import errors";
    }

    @Test
    public void testTypeImportOnDemandNameClash () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import java.awt.*;\n" +
			    "import java.util.*;\n" +
			    "class Qwer { List l; }\n");
	assert diagnostics.hasError () : "Expected import errors";
    }

    @Test
    public void testNonFoundPackageAndClass () throws IOException {
	parseAndSetClasses ("import bar.Bar;\n" +
			    "class Foo {}\n");
	assert diagnostics.hasError () : "Expected import errors";
    }

    @Test
    public void testNonFoundClass () throws IOException {
	parseAndSetClasses ("import java.util.QEWR;\n" +
			    "class Foo {}\n");
	assert diagnostics.hasError () : "Expected import errors";
    }

    @Test
    public void testAmbigous () throws IOException {
	parseAndSetClasses ("package a; public class A {}",
			    "package b; public class A {}",
			    "package c; import a.*; import b.*; class C { A a; }");
	assert diagnostics.hasError () : "Expected ambigous class";
	diagnostics = new CompilerDiagnosticCollector ();

	// same package wins over star import
	parseAndSetClasses ("package foo; import bar.*; class A {} class Foo { A a;}",
			    "package bar; class A {}");
	assertNoErrors ();
    }

    @Test
    public void testAmbigousIgnoringAccessResource () throws IOException {
	// java.awt.EventQueue contains Queue-class, but with default access
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.*;\n" +
			    "import java.awt.*;\n" +
			    "class Foo { Queue q; }\n");
	assertNoErrors ();
    }

    @Test
    public void testAmbigousIgnoringAccessCompiled () throws IOException {
	parseAndSetClasses ("package a; class F {}\n",
			    "package b; public class F {}\n",
			    "package c; import a.*; import b.*; class C { F f; }");
	assertNoErrors ();
    }

    @Test
    public void testStaticImportWins () throws IOException {
	parseAndSetClasses ("package a; public class I {}",
			    "package b; public class B { public static class I {}}",
			    "package c; import a.*; import b.B.*; import static b.B.I; class C { I i; }");
	assertNoErrors ();
	checkField ("c.C", 0, "b.B$I");
    }

    @Test
    public void testMethodParams () throws IOException {
	parseAndSetClasses ("class Foo { String foo (String a, String b) { return a + b; }}");
	assertNoErrors ();
    }

    @Test
    public void testInnerClasses () throws IOException {
	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { static void a () { new A ().new B ();}}");
	assertNoErrors ();
	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { static A a = new A ();\n" +
			    "                       static void a () { a.new B ();}}");
	assertNoErrors ();
	// same as above, but different order
	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { static void a () { a.new B ();}\n" +
			    "                       static A a = new A ();}");
	assertNoErrors ();

	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { A a = null;\n" +
			    "                       void a () { a.new B ();}}");
	assertNoErrors ();
	// same as above, but different order
	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { void a () { a.new B ();}\n" +
			    "                       A a = new A ();}");
	assertNoErrors ();

	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { A a = new A ();\n" +
			    "                       static void a () { a.new B ();}}");
	assert diagnostics.hasError () : "Can not use non static field in static method";
    }

    @Test
    public void testCastType () throws IOException {
	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { static Object a = new A();\n" +
			    "                       static void a () { ((A)a).new B ();}}");
	assertNoErrors ();
	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { static Object a = new A();\n" +
			    "                       static void a () { ((A)a).new B () {};}}");
	assertNoErrors ();
    }

    @Test
    public void testInnerLocalFieldTypes () throws IOException {
	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { void a () { A a = new A(); a.new B ();}}");
	assertNoErrors ();
    }

    @Test
    public void testInnerMethodFieldType () throws IOException {
	parseAndSetClasses ("package foo; class A { class B {}}",
			    "package foo; class C { void a (A a) { a.new B ();}}");
	assertNoErrors ();
    }

    @Test
    public void testIfScope () throws IOException {
	parseAndSetClasses ("package foo; class A { void a () {\n" +
			    "    boolean b = false;\n" +
			    "    if (b) { String a = \"foo\"; }\n" +
			    "    if (b) { String a = \"bar\"; }\n" +
			    "}}");
	assertNoErrors ();
    }

    @Test
    public void testForScope () throws IOException {
	parseAndSetClasses ("package foo; class A { void a () {\n" +
			    "    for (int i = 0; i < 3; i++) { String a = \"foo\"; }\n" +
			    "    for (int i = 0; i < 3; i++) { String a = \"foo\"; }\n" +
			    "}}");
	assertNoErrors ();
    }

    @Test
    public void testAccessToProtectedInnerFromBase () throws IOException {
	parseAndSetClasses ("package a; public class A { protected enum E {}}",
			    "package b; import a.*; class B extends A { E e; }");
	assertNoErrors ();
	parseAndSetClasses ("package a; class A { protected enum E {}}",
			    "package a; public class B extends A {}",
			    "package c; import a.B; class C extends B { E e; }");
	assertNoErrors ();
	parseAndSetClasses ("package a; public class A { protected enum E {}}",
			    "package b; class B { a.A.E e; }");
	assert diagnostics.hasError () : "a.A.E is protected so can not be used in B";
    }

    @Test
    public void testAccessToPrivateInner () throws IOException {
	parseAndSetClasses ("package a; public class A { void a () { return new E ();} private class E {} }");
	assertNoErrors ();
    }

    @Test
    public void testImplementsMember () throws IOException {
	// Tests two things
	// 1: implemented interfaces are part of super types
	// 2: interface members are public when no access level is given
	parseAndSetClasses ("package a; public interface A { static enum E {}}",
			    "package b; import a.A; class B implements A { E e; }");
	assertNoErrors ();
    }

    @Test
    public void testAnonymousAccess () throws IOException {
	parseAndSetClasses ("package a; public class A { protected class E {}}",
			    "package b; import a.A; class B { void f () { new A() { E e; }; }}");
	assertNoErrors ();

	parseAndSetClasses ("package a; public class A { protected class E {}}",
			    "package b; import a.A; class B { void f () { new A() { void e () {E e;}}; }}");
	assertNoErrors ();
    }

    @Test
    public void testOuterInheritedInnerClassAccess () throws IOException {
	parseAndSetClasses ("package a; public class A { protected static class E {}}",
			    "package b; import a.A; class B extends A { public class C { void f () { E e; }}}");
	assertNoErrors ();
    }

    @Test
    public void testClassTypeParameters () throws IOException {
	parseAndSetClasses ("class A<T extends Runnable & java.io.Serializable> {}");
	assertNoErrors ();
	NormalClassDeclaration cd = (NormalClassDeclaration)cip.getType ("A");
	checkTypeParameters (cd.getTypeParameters (), 1, "java.lang.Runnable", "java.io.Serializable");
    }

    @Test
    public void testMethodTypeParameters () throws IOException {
	parseAndSetClasses ("import java.io.InputStream;\n" +
			    "import java.io.Serializable;\n" +
			    "class A {\n" +
			    "    <T extends InputStream & Serializable> T create () { return null; }\n" +
			    "}");
	assertNoErrors ();
	NormalClassDeclaration cd = (NormalClassDeclaration)cip.getType ("A");
	ClassBody body = cd.getBody ();
	MethodDeclaration m = (MethodDeclaration)body.getDeclarations ().get (0);
	checkTypeParameters (m.getTypeParameters (), 1, "java.io.InputStream", "java.io.Serializable");
    }

    @Test
    public void testMissingField () throws IOException {
	parseAndSetClasses ("class A { void a () { return b; }}");
	assert diagnostics.hasError () : "Should not be able to find b";

	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class A { void a () { int a = 0; return a + b; }}");
	assert diagnostics.hasError () : "Should not be able to find b";

	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class A { void a () { int a = 0; int c = b; }}");
	assert diagnostics.hasError () : "Should not be able to find b";

	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class A { void a () { int a = 0; int c = a + b; }}");
	assert diagnostics.hasError () : "Should not be able to find b";
    }

    @Test
    public void testMethodWithNewInstanceParameters () throws IOException {
	parseAndSetClasses ("class A { void a (A a) {} void b () { a (new A ()); }}");
	assertNoErrors ();
    }

    @Test
    public void testVarArgConstructor () throws IOException {
	parseAndSetClasses ("class A { public A(int... is) {}}");
	assertNoErrors ();
    }

    @Test
    public void testStaticImportStar () throws IOException {
	parseAndSetClasses ("import static java.lang.Math.*; class A { double p2 = PI; }");
	assertNoErrors ();
    }

    @Test
    public void testStaticImportField () throws IOException {
	parseAndSetClasses ("import static java.lang.Math.PI; class A { double p2 = PI; }");
	assertNoErrors ();
    }

    @Test
    public void testInvalidStaticImport () throws IOException {
	parseAndSetClasses ("import static java.lang.Math.PO; class A {  }");
	assert diagnostics.hasError () : "Expected import errors";
    }

    @Test
    public void testLambdaScopeWithoutIdentifier () throws IOException {
	parseAndSetClasses ("class A { Runnable r = () -> {}; }");
	assertNoErrors ();
    }

    @Test
    public void testLambdaScopeIdentifier () throws IOException {
	parseAndSetClasses ("class A { Comparable<?> c = o -> o.hashCode (); }");
	assertNoErrors ();
    }

    @Test
    public void testLambdaScopeInferred () throws IOException {
	parseAndSetClasses ("class A { Comparable<?> c = (o) -> o.hashCode (); }");
	assertNoErrors ();
    }

    @Test
    public void testLambdaScopeFormal () throws IOException {
	parseAndSetClasses ("class A { Comparable<?> c = (Object o) -> o.hashCode (); }");
	assertNoErrors ();
    }

    @Test
    public void testDirectInheritedField () throws IOException {
	parseAndSetClasses ("public class A { protected A a; }",
			    "class B extends A { void b () { a = new B (); }}");
	assertNoErrors ();
    }

    @Test
    public void testIndirectInheritedField () throws IOException {
    	parseAndSetClasses ("public class A { protected A a; }",
			    "class B extends A {}",
			    "class C extends B { void c () { a = new C (); }}");
	assertNoErrors ();
    }

    @Test
    public void testInnerClassFieldsConstructorUsage () throws IOException {
    	parseAndSetClasses ("class A { public class B { Object t; public B () { t = new Object ();}}}");
	assertNoErrors ();
    	parseAndSetClasses ("class A { public static class B { Object t; public B () { t = new Object ();}}}");
	assertNoErrors ();
    }

    @Test
    public void testCatchParameters () throws IOException {
    	parseAndSetClasses ("class A { void f () { try { int i = 0; } catch (Exception e) { e.printStackTrace (); }}}");
	assertNoErrors ();
    	parseAndSetClasses ("class A { void f () { try { int i = 0; } " +
			    "catch (NullPointerException | IllegalArgumentException e) { " +
			    "e.printStackTrace (); }}}");
	assertNoErrors ();
    	parseAndSetClasses ("class A { void f () { try { int i = 0; } " +
			    "catch (IllegalArgumentException e) { e.printStackTrace (); } " +
			    "catch (NullPointerException r) { r.printStackTrace ();}}}");
	assertNoErrors ();
    	parseAndSetClasses ("class A { void f () { try { int i = 0; } " +
			    "catch (IllegalArgumentException e) { e.printStackTrace (); } " +
			    "catch (NullPointerException r) { e.printStackTrace ();}}}"); // Intentional typo
	assert diagnostics.hasError () : "Expected import errors";
    }

    @Test
    public void testAssignFinalField () throws IOException {
	parseAndSetClasses ("class Foo { final int bar = 4; }");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { final int bar = 4; void foo () { bar = 3; }}");
	assertErrors ();
    }

    @Test
    public void testAssignFinalFieldInConstructor () throws IOException {
	parseAndSetClasses ("class Foo { final int bar; Foo () { bar = 3; }}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { final int bar; Foo () { bar = 3; } Foo (int i) { bar = 5; }}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { final int bar; Foo () { bar = 3; bar = 4;}}");
	assertErrors ();
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { final int bar = 5; Foo () { bar = 3;}}");
	assertErrors ();
    }

    @Test
    public void testAssignFinalFieldInConstructorIfs () throws IOException {
	parseAndSetClasses ("class Foo { final int bar; Foo () { if (true) bar = 3; }}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { final int bar; Foo (boolean b) { if (b) bar = 3; }}");
	assertErrors ();
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { final int bar; Foo (boolean b) { if (b) bar = 3; else bar = 4; }}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { final int bar; Foo (boolean a, boolean b) { if (a) { if (b) bar = 1; else bar = 2; } else { bar = 4; }}}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { final int bar; Foo (boolean a, boolean b) { if (a) { if (b) bar = 1; } else bar = 4; }}");
	assertErrors ();
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { final int bar; Foo (boolean b) { if (b) bar = 3; bar = 4; }}");
	assertErrors ();
	diagnostics = new CompilerDiagnosticCollector ();

	// Need to figure out that T is true
	// parseAndSetClasses ("class Foo { final int bar; static final boolean T = true; Foo () { if (T) bar = 3; }}");
	// assertNoErrors ();
    }

    @Test
    public void testAssignFinalFieldInConstructorTry () throws IOException {
	// javac fails with "bar might not have been initialized after catch block"
	parseAndSetClasses ("class Foo { final int bar; Foo () { try { bar = 3; } catch (Exception e) {}}}");
	assertErrors ();
	diagnostics = new CompilerDiagnosticCollector ();

	// javac fails with "bar might already have been assigned" in catch block
	parseAndSetClasses ("class Foo { final int bar; Foo () { try { bar = 3; } catch (Exception e) { bar = 4; }}}");
	assertErrors ();
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { final int bar; Foo () { try { } finally { bar = 4; }}}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { final int bar; Foo () { try { } catch (Exception e) { } finally { bar = 4; }}}");
	assertNoErrors ();

	parseAndSetClasses ("class Foo { final int bar; Foo () { try { } catch (Exception e) { bar = 3; } finally { bar = 4; }}}");
	assertErrors ();
	diagnostics = new CompilerDiagnosticCollector ();

	parseAndSetClasses ("class Foo { final int bar; Foo () { try { bar = 3; } catch (Exception e) { bar = 3; }finally { bar = 4; }}}");
	assertErrors ();
    }

    @Test
    public void testAssignFinalVariable () throws IOException {
	parseAndSetClasses ("class Foo { void foo () { final int bar = 3; }}");
	assertNoErrors ();
	parseAndSetClasses ("class Foo { void foo () { final int bar = 3; bar = 4; }}");
	assertErrors ();
    }

    @Test
    public void testAssignFinalParameter () throws IOException {
	parseAndSetClasses ("class Foo { void foo (final int bar) { bar = 3; }}");
	assertErrors ();
    }

    @Test
    public void testUnassignedFinalField () throws IOException {
	parseAndSetClasses ("class Foo { final int bar; }");
	assertErrors ();

	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("class Foo { final int bar; Foo () {}}");
	assertErrors ();
    }

    private void checkTypeParameters (TypeParameters tps, int numParams,
				      String bound1, String additionalBound) {
	List<TypeParameter> ls = tps.get ();
	assert ls.size () == numParams;
	TypeParameter tp = ls.get (0);
	TypeBound b = tp.getTypeBound ();
	assert b != null;
	assert b.getType () != null;
	assert b.getType ().getFullName ().equals (bound1);
	List<AdditionalBound> abs = b.getAdditionalBounds ();
	assert abs != null;
	assert abs.size () == 1;
	assert abs.get (0).getType ().getFullName ().equals (additionalBound);
    }

    private void checkImplements (String classToCheck, String wantedInterface) {
	NormalClassDeclaration cd = (NormalClassDeclaration)cip.getType (classToCheck);
	checkOneInterface (cd.getSuperInterfaces (), wantedInterface);
    }

    private void checkSuperTypeName (String classToCheck, String expectedSuperType) {
	NormalClassDeclaration cd = (NormalClassDeclaration)cip.getType (classToCheck);
	assertClassType (cd.getSuperClass (), expectedSuperType);
    }

    private void checkOneInterface (InterfaceTypeList ifs, String type) {
	assert ifs != null;
	List<ClassType> cts = ifs.get ();
	assert cts != null;
	assert cts.size () == 1;
	String name = cts.get (0).getFullName ();
	assert type.equals (name) : "Wrong name, got: " + name + ", expected: " + type;
    }

    private void checkField (String className, int bodyPosition, String expectedType) {
	NormalClassDeclaration cd = (NormalClassDeclaration)cip.getType (className);
	ClassBody body = cd.getBody ();
	FieldDeclaration fd = (FieldDeclaration)body.getDeclarations ().get (bodyPosition);
	assertClassType ((ClassType)fd.getType (), expectedType);
    }

    private void assertClassType (ClassType ct, String expectedType) {
	assert ct.getFullName ().equals (expectedType) : "Got wrong type: " +
	    ct.getFullName () + ", expected: " + expectedType;
    }
}