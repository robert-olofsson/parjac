package org.khelekore.parjac.parser;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.GrammarReader;
import org.khelekore.parjac.lexer.CharBufferLexer;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;

public class TestParseHelper {

    public static Grammar getJavaGrammarFromFile (String goalRule) throws IOException {
	GrammarReader gr = new GrammarReader (true);
	gr.read (TestParseHelper.class.getResource ("/java_8.pj"));
	Grammar g = gr.getGrammar ();
	g.addRule ("Goal", goalRule, Token.END_OF_INPUT);
	try {
	    g.validateRules ();
	} catch (Throwable t) {
	    t.printStackTrace ();
	}
	return g;
    }

    public static void parse (LRParser lr, String s, CompilerDiagnosticCollector diagnostics) {
	Parser p = getParser (lr, s, diagnostics);
	if (lr.getDebug ())
	    System.out.println ("Trying to parse: " + s);
	p.parse ();
    }

    private static Parser getParser (LRParser lr, String s, CompilerDiagnosticCollector diagnostics) {
	CharBuffer charBuf = CharBuffer.wrap (s);
	Path path = Paths.get ("TestParseHelper.getParser");
	Lexer lexer = new CharBufferLexer (charBuf);
	Parser p = new Parser (lr, path, lexer, diagnostics, lr.getDebug ());
	return p;
    }

    public static void earleyParse (Grammar g, String s, CompilerDiagnosticCollector diagnostics) {
	CharBuffer charBuf = CharBuffer.wrap (s);
	Path path = Paths.get ("TestParseHelper.getParser");
	Lexer lexer = new CharBufferLexer (charBuf);
	PredictCache pc = new PredictCache (g); // TODO: this is pretty inefficient
	EarleyParser ep = new EarleyParser (g, path, lexer, pc, diagnostics, false);
	ep.parse ();
    }

    public static String getParseOutput (CompilerDiagnosticCollector diagnostics) {
	Locale l = Locale.getDefault ();
	return diagnostics.getDiagnostics ().
	    map (d -> d.getMessage (l)).
	    collect (Collectors.joining ("\n"));
    }
}