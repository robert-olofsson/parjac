package org.khelekore.parjac.parser;

import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.lexer.CharBufferLexer;
import org.khelekore.parjac.lexer.Lexer;

public class TestParseHelper {
    public static void parse (LRParser lr, String s, CompilerDiagnosticCollector diagnostics) {
	Parser p = getParser (lr, s, diagnostics);
	if (lr.getDebug ())
	    System.out.println ("Trying to parse: " + s);
	p.parse ();
    }

    private static Parser getParser (LRParser lr, String s, CompilerDiagnosticCollector diagnostics) {
	CharBuffer charBuf = CharBuffer.wrap (s);
	Path path = Paths.get ("TestParseHelper.getParser");
	Lexer lexer = new CharBufferLexer (path, charBuf);
	Parser p = new Parser (lr, path, lexer, diagnostics, lr.getDebug ());
	return p;
    }

    public static void earleyParse (Grammar g, String s, CompilerDiagnosticCollector diagnostics) {
	CharBuffer charBuf = CharBuffer.wrap (s);
	Path path = Paths.get ("TestParseHelper.getParser");
	Lexer lexer = new CharBufferLexer (path, charBuf);
	EarleyParser ep = new EarleyParser (g, path, lexer, diagnostics, false);
	ep.parse ();
    }

    public static String getParseOutput (CompilerDiagnosticCollector diagnostics) {
	Locale l = Locale.getDefault ();
	return diagnostics.getDiagnostics ().
	    map (d -> d.getMessage (l)).
	    collect (Collectors.joining ("\n"));
    }
}