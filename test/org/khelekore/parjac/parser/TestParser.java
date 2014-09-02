package org.khelekore.parjac.parser;

import java.nio.CharBuffer;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.lexer.Lexer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestParser {
    @Test
    public void testEmpty () {
	testParse ("");
    }

    @Test
    public void testPackage () {
	testParse ("package foo;");
	testParseError ("package foo");
	testParse ("package foo.bar;");
	testParseError ("package foo.bar");
	testParse ("@Foo package foo;");
	testParse ("@Foo(a = \"hello\") package foo;");
    }

    @Test
    public void testImport () {
	testParse ("import foo;");
	testParseError ("import foo");
	testParse ("import foo.Bar;");
	testParse ("import foo.*;");
	testParse ("import foo.bar.*;");
	testParse ("import foo.bar.Baz;");
	testParse ("import static foo;");
	testParse ("import static foo.bar;");
	testParse ("import static foo.bar.Baz;");
	testParse ("import static foo.bar.Baz.*;");
    }

    private void testParse (String text) {
	Parser p = getParser (text);
	p.parse ();
	checkNoErrors (p);
    }

    private void testParseError (String text) {
	Parser p = getParser (text);
	p.parse ();
	assert p.getDiagnostics ().hasError ();
    }

    private Parser getParser (String text) {
	CharBuffer cb = CharBuffer.wrap (text.toCharArray ());
	Path path = Paths.get ("TestParser");
	Lexer lexer = new Lexer (path, cb);
	CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	return new Parser (path, lexer, diagnostics);
    }

    private void checkNoErrors (Parser p) {
	CompilerDiagnosticCollector diagnostics = p.getDiagnostics ();
	assert !diagnostics.hasError () : "Got errors:\n" + getErrors (diagnostics);
    }

    private String getErrors (CompilerDiagnosticCollector diagnostics) {
	StringBuilder sb = new StringBuilder ();
	Locale locale = Locale.getDefault ();
	diagnostics.getDiagnostics ().
	    forEach (d -> append (sb, d.getMessage (locale)));
	return sb.toString ();
    }

    private void append (StringBuilder sb, String msg) {
	sb.append (msg);
	sb.append ("\n");
    }
}