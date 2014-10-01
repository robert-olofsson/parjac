package org.khelekore.parjac.parser;

import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.lexer.CharBufferLexer;
import org.khelekore.parjac.lexer.Lexer;

public class TestParseHelper {
    public static void parse (LRParser lr, String s, CompilerDiagnosticCollector diagnostics) {
	Parser p = getParser (lr, s, diagnostics);
	p.parse ();
    }

    private static Parser getParser (LRParser lr, String s, CompilerDiagnosticCollector diagnostics) {
	CharBuffer charBuf = CharBuffer.wrap (s);
	Path path = Paths.get ("TestImports");
	Lexer lexer = new CharBufferLexer (path, charBuf);
	Parser p = new Parser (lr, path, lexer, diagnostics, false);
	return p;
    }

    public static String getParseOutput (CompilerDiagnosticCollector diagnostics) {
	Locale l = Locale.getDefault ();
	return diagnostics.getDiagnostics ().
	    map (d -> d.getMessage (l)).
	    collect (Collectors.joining ("\n"));
    }
}