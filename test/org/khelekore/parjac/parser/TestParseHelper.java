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
import org.khelekore.parjac.tree.SyntaxTree;

public class TestParseHelper {
    private static final Grammar baseGrammar;

    static {
	try {
	    GrammarReader gr = new GrammarReader ();
	    gr.read (TestParseHelper.class.getResource ("/java_8.pj"));
	    baseGrammar = gr.getGrammar ();
	} catch (IOException e) {
	    throw new RuntimeException ("Failed to read grammar", e);
	}
    }

    public static Grammar getJavaGrammarFromFile (String goalRule, boolean allowMany) {
	Grammar g = new Grammar (baseGrammar);
	if (allowMany) {
	    g.addRule ("Goalp", g.zeroOrMore (goalRule));
	    g.addRule ("Goal", "Goalp", Token.END_OF_INPUT);
	} else {
	    g.addRule ("Goal", goalRule, Token.END_OF_INPUT);
	}
	try {
	    g.validateRules ();
	} catch (Throwable t) {
	    t.printStackTrace ();
	}
	return g;
    }

    public static void earleyParse (Grammar g, String s, CompilerDiagnosticCollector diagnostics) {
	parse (g, s, diagnostics, null);
    }

    public static SyntaxTree earleyParseBuildTree (Grammar g, String s, CompilerDiagnosticCollector diagnostics) {
	return parse (g, s, diagnostics, new JavaTreeBuilder (g));
    }

    private static SyntaxTree parse (Grammar g, String s, CompilerDiagnosticCollector diagnostics,
				     JavaTreeBuilder tb) {
	CharBuffer charBuf = CharBuffer.wrap (s);
	Path path = Paths.get ("TestParseHelper.getParser");
	Lexer lexer = new CharBufferLexer (charBuf);
	PredictCache pc = new PredictCache (g); // TODO: this is pretty inefficient
	EarleyParser ep = new EarleyParser (g, path, lexer, pc, tb, diagnostics, false);
	return ep.parse ();
    }

    public static String getParseOutput (CompilerDiagnosticCollector diagnostics) {
	Locale l = Locale.getDefault ();
	return diagnostics.getDiagnostics ().
	    map (d -> d.getMessage (l)).
	    collect (Collectors.joining ("\n"));
    }
}