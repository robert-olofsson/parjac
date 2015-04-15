package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.parser.TestParseHelper;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExtendsInterfaces;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.InterfaceTypeList;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
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
	checkField ("foo.bar.Foo", 1, "foo.bar.Foo.Bar");
    }

    @Test
    public void testEnumExtends () throws IOException {
	parseAndSetClasses ("package foo.bar; interface Foo {} enum E implements Foo {A, B}");
	assertNoErrors ();
	EnumDeclaration ed = (EnumDeclaration)cth.getType ("foo.bar.E");
	checkOneInterface (ed.getSuperInterfaces (), "foo.bar.Foo");
    }

    @Test
    public void testInterfaceExtends () throws IOException {
	parseAndSetClasses ("package foo.bar; interface Foo {} interface Bar extends Foo {}");
	assertNoErrors ();
	NormalInterfaceDeclaration id = (NormalInterfaceDeclaration)cth.getType ("foo.bar.Bar");
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
    public void testGenericMethodType () throws IOException {
	parseAndSetClasses ("package foo;\nclass Foo {\n<T> T foo (T t) { T t; }}");
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

    /*
    @Test
    public void testInnerClassFromSuperClassKnown () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import java.util.HashMap;\n" +
			    "class Foo extends HashMap { Entry e = null; }");
	assertNoErrors ();
    }
    */

    @Test
    public void testSingleStaticImport () throws IOException {
	parseAndSetClasses ("package foo;\n" +
			    "import static java.util.Map.Entry;\n" +
			    "class Foo {Entry e = null;}");
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

    private void parseAndSetClasses (String code) {
	SyntaxTree st = TestParseHelper.earleyParseBuildTree (g, code, diagnostics);
	assert st != null : "Failed to parse:"  + code + ": " + getDiagnostics ();
	cth.addTypes (st);
	ClassSetter cs = new ClassSetter (cth, crh, st, null);
	cs.fillIn ();
	cs = new ClassSetter (cth, crh, st, diagnostics);
	cs.fillIn ();
    }

    private void checkSuperTypeName (String classToCheck, String expectedSuperType) {
	NormalClassDeclaration cd = (NormalClassDeclaration)cth.getType (classToCheck);
	assertClassType (cd.getSuperClass (), expectedSuperType);
    }

    private void checkOneInterface (InterfaceTypeList ifs, String type) {
	assert ifs != null;
	List<ClassType> cts = ifs.get ();
	assert cts != null;
	assert cts.size () == 1;
	assert cts.get (0).getFullName ().equals (type);
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
	assert !diagnostics.hasError () : "Got errors: " + getDiagnostics ();
    }

    private String getDiagnostics () {
	Locale loc = Locale.getDefault ();
	return diagnostics.getDiagnostics ().
	    map (c -> c.getMessage (loc)).
	    collect (Collectors.joining ("\n"));
    }
}