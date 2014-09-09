package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;
import org.testng.annotations.Test;

public class TestParser {
    @Test
    public void testEmpty () {
	testParse ();
    }

    @Test
    public void testPackage () {
	// package foo
	testParseError (Token.PACKAGE, Token.IDENTIFIER);
	// package foo;
	testParse (Token.PACKAGE, Token.IDENTIFIER, Token.SEMICOLON);
	// package foo.bar
	testParseError (Token.PACKAGE, Token.IDENTIFIER, Token.DOT, Token.IDENTIFIER);
	// package foo.bar;
	testParse (Token.PACKAGE, Token.IDENTIFIER, Token.DOT, Token.IDENTIFIER, Token.SEMICOLON);
	// @Foo package foo;
	testParse (Token.AT, Token.IDENTIFIER, Token.PACKAGE,
		   Token.IDENTIFIER, Token.DOT, Token.IDENTIFIER, Token.SEMICOLON);
	/*
	// @Foo(a = "hello") package foo;
	testParse (Token.AT, Token.IDENTIFIER, Token.LEFT_PARENTHESIS, Token.IDENTIFIER,
		   Token.EQUAL, Token.STRING_LITERAL, Token.RIGHT_PARENTHESIS,
		   Token.PACKAGE, Token.IDENTIFIER, Token.SEMICOLON);
	*/
    }

    @Test
    public void testImport () {
	// import foo
	testParseError (Token.IMPORT, Token.IDENTIFIER);
	// import foo;
	testParse (Token.IMPORT, Token.IDENTIFIER, Token.SEMICOLON);
	// import foo.Bar;
	testParse (Token.IMPORT, Token.IDENTIFIER, Token.DOT, Token.IDENTIFIER, Token.SEMICOLON);
	// import foo.*;
	testParse (Token.IMPORT, Token.IDENTIFIER, Token.DOT, Token.MULTIPLY, Token.SEMICOLON);
	// import foo.bar.*;
	testParse (Token.IMPORT, Token.IDENTIFIER, Token.DOT, Token.IDENTIFIER, Token.DOT,
		   Token.MULTIPLY, Token.SEMICOLON);
	// import foo.bar.Baz;
	testParse (Token.IMPORT, Token.IDENTIFIER, Token.DOT, Token.IDENTIFIER, Token.DOT,
		   Token.IDENTIFIER, Token.SEMICOLON);
	// import static foo;
	testParse (Token.IMPORT, Token.STATIC, Token.IDENTIFIER, Token.SEMICOLON);
	// import static foo.bar;
	testParse (Token.IMPORT, Token.STATIC, Token.IDENTIFIER, Token.DOT,
		   Token.IDENTIFIER, Token.SEMICOLON);
	// import static foo.bar.Baz;
	testParse (Token.IMPORT, Token.STATIC, Token.IDENTIFIER, Token.DOT, Token.IDENTIFIER,
		   Token.DOT, Token.IDENTIFIER, Token.SEMICOLON);
	// import static foo.bar.Baz.*;
	testParse (Token.IMPORT, Token.STATIC, Token.IDENTIFIER, Token.DOT, Token.IDENTIFIER,
		   Token.DOT, Token.IDENTIFIER, Token.DOT, Token.MULTIPLY, Token.SEMICOLON);
    }

    @Test
    public void testClass () {
	testParse (Token.CLASS, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
	testParse (Token.PUBLIC, Token.CLASS, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
	testParse (Token.ABSTRACT, Token.CLASS, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
	/*
	testParse (Token.ABSTRACT, Token.CLASS, Token.IDENTIFIER, Token.LEFT_CURLY,
		   Token.INT, Token.IDENTIFIER, Token.EQUAL, Token.INT_LITERAL, Token.SEMICOLON,
		   Token.RIGHT_CURLY);
	*/
    }

    @Test
    public void testEnum () {
	testParse (Token.ENUM, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
	testParse (Token.PUBLIC, Token.INTERFACE, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
    }

    @Test
    public void testInterface () {
	testParse (Token.INTERFACE, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
	testParse (Token.PUBLIC, Token.INTERFACE, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
    }

    /* TODO: currently broken, match the anotations before package
    @Test
    public void testAnnotation () {
	testParse (Token.AT, Token.INTERFACE, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
	testParse (Token.PACKAGE, Token.IDENTIFIER, Token.SEMICOLON,
		   Token.AT, Token.INTERFACE, Token.IDENTIFIER, Token.LEFT_CURLY, Token.RIGHT_CURLY);
    }
    */

    private void testParse (Token... tokens) {
	Parser p = getParser (tokens);
	p.parse ();
	checkNoErrors (p);
    }

    private void testParseError (Token... tokens) {
	Parser p = getParser (tokens);
	p.parse ();
	assert p.getDiagnostics ().hasError ();
    }

    private Parser getParser (Token... tokens) {
	Lexer lexer = new ListLexer (tokens);
	CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	Path path = Paths.get ("TestParser");
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

    private static class ListLexer implements Lexer {
	private final Iterator<Token> tokens;

	public ListLexer (Token... tokens) {
	    this.tokens = Arrays.asList (tokens).iterator ();
	}

	public String getError () {
	    throw new RuntimeException ("No error should appear");
	}

	public char getCharValue () {
	    return 'c';
	}

	public String getStringValue () {
	    return "SomeString";
	}

	public int getIntValue () {
	    return 42;
	}

	public long getLongValue () {
	    return 1729L;
	}

	public float getFloatValue () {
	    return 3.14f;
	}

	public double getDoubleValue () {
	    return 10.0;
	}

	public String getIdentifier () {
	    return "someId";
	}

	public long getTokenStartPos () {
	    return 0;
	}

	public long getTokenEndPos () {
	    return 0;
	}

	public long getLineNumber () {
	    return 1;
	}

	public long getTokenColumn () {
	    return 1;
	}

	public boolean hasMoreTokens () {
	    return tokens.hasNext ();
	}

	public Token nextToken () {
	    return tokens.next ();
	}

	public void setInsideTypeContext (boolean insideTypeContext) {
	    // ignore
	}
    }
}