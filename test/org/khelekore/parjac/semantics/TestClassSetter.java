package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExtendsInterfaces;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.InterfaceTypeList;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SimpleClassType;
import org.khelekore.parjac.tree.TypeArguments;
import org.testng.annotations.Test;

public class TestClassSetter extends TestBase {

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
    public void testInnerClass () throws IOException {
	parseAndSetClasses ("package foo.bar; class Foo { class Bar {} Bar bar;}");
	assertNoErrors ();
	checkField ("foo.bar.Foo", 1, "foo.bar.Foo$Bar");
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
			    "    <T> T foo (T t) { T t; }\n" +
			    "}");
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
	parseAndSetClasses ("package a; class A {}",
			    "package b; class A {}",
			    "package c; import a.*; import b.*; class C { A a; }");
	assert diagnostics.hasError () : "Expected ambigous class";
	diagnostics = new CompilerDiagnosticCollector ();

	// same package wins over star import
	parseAndSetClasses ("package foo; import bar.*; class A {} class Foo { A a;}",
			    "package bar; class A {}");
	assertNoErrors ();
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