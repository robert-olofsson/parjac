package org.khelekore.parjac.grammar.example;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.lexer.CharBufferLexer;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.parser.LRParser;
import org.khelekore.parjac.parser.Parser;

import static org.khelekore.parjac.lexer.Token.*;

public class Trivial {
    private final Grammar gr;
    private final LRParser lr;

    public Trivial () {
	gr = new Grammar ();
	addRules ();
	lr = new LRParser (gr, true);
	lr.build ();
    }

    public static void main (String[] args) throws IOException {
	Trivial g = new Trivial ();
	Locale locale = Locale.getDefault ();
	for (String s : args) {
	    CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	    Path path = Paths.get (s);
	    ByteBuffer buf = ByteBuffer.wrap (Files.readAllBytes (path));
	    CharsetDecoder decoder = Charset.forName ("UTF-8").newDecoder ();
	    CharBuffer charBuf = decoder.decode (buf);
	    Lexer lexer = new CharBufferLexer (charBuf);
	    Parser p = new Parser (g.lr, path, lexer, diagnostics, false);
	    p.parse ();
	    diagnostics.getDiagnostics ().
		forEach (d -> System.err.println (d.getMessage (locale)));
	}
    }

    private void addRules () {
	gr.addRule ("Goal", "E");
	gr.addRule ("E", gr.zeroOrMore ("M"), gr.zeroOrOne (DEFAULT), gr.zeroOrMore ("M"),
		    gr.zeroOrOne (DEFAULT), gr.zeroOrMore ("M"), DOT);
	gr.addRule ("M", IDENTIFIER);
	/*
	gr.addRule ("Goal", "CompilationUnit");
	gr.addRule ("CompilationUnit",
		    gr.zeroOrMore ("ImportDeclaration"));
	gr.addRule ("ImportDeclaration",
		    gr.oneOf ("SingleTypeImportDeclaration",
			      "TypeImportOnDemandDeclaration",
			      "SingleStaticImportDeclaration",
			      "StaticImportOnDemandDeclaration"));
	gr.addRule ("SingleTypeImportDeclaration",
		    IMPORT, "TypeName", SEMICOLON);
	gr.addRule ("TypeImportOnDemandDeclaration",
		    IMPORT, "TypeName", DOT, MULTIPLY, SEMICOLON);
	gr.addRule ("SingleStaticImportDeclaration",
		    IMPORT, STATIC, "TypeName", DOT, IDENTIFIER, SEMICOLON);
	gr.addRule ("StaticImportOnDemandDeclaration",
		    IMPORT, STATIC, "TypeName", DOT, MULTIPLY, SEMICOLON);
	gr.addRule ("TypeName",
		    gr.oneOf (IDENTIFIER,
			      gr.sequence ("TypeName", DOT, IDENTIFIER)));
	*/
	/*
	gr.addRule ("Goal", "E");
	gr.addRule ("E", "E", PLUS, "T");
	gr.addRule ("E", "T");
	gr.addRule ("T", "T", MULTIPLY, "P");
	gr.addRule ("T", "P");
	gr.addRule ("P", IDENTIFIER);
	gr.addRule ("P", LEFT_PARENTHESIS, "E", RIGHT_PARENTHESIS);

	gr.addRule ("Goal", "S");
	gr.addRule ("S", "E", EQUAL, "E");
	gr.addRule ("S", IDENTIFIER);
	gr.addRule ("E", "E", PLUS, IDENTIFIER);
	gr.addRule ("E", IDENTIFIER);

	gr.addRule ("Goal", "E");
	gr.addRule ("E", "T", PLUS, "E");
	gr.addRule ("E", "T");
	gr.addRule ("T", IDENTIFIER);

	gr.addRule ("Goal", "E");
	gr.addRule ("E", "E", PLUS, "T");
	gr.addRule ("E", "E", MINUS, "T");
	gr.addRule ("E", "T");
	gr.addRule ("T", "T", MULTIPLY, "F");
	gr.addRule ("T", "T", DIVIDE, "F");
	gr.addRule ("T", "F");
	gr.addRule ("F", LEFT_PARENTHESIS, "E", RIGHT_PARENTHESIS);
	gr.addRule ("F", IDENTIFIER);

	gr.addRule ("Goal", "E", END_OF_INPUT);
	gr.addRule ("E", "E", PLUS, "T");
	gr.addRule ("E", "T");
	gr.addRule ("T", "T", MULTIPLY, "P");
	gr.addRule ("T", "P");
	gr.addRule ("P", IDENTIFIER);
	gr.addRule ("P", LEFT_PARENTHESIS, "E", RIGHT_PARENTHESIS);

	//
	addRule ("Goal", "S", END_OF_INPUT);
	addRule ("S", "L", EQUAL, "R");
	addRule ("S", "R");
	addRule ("L", MULTIPLY, "R");
	addRule ("L", IDENTIFIER);
	addRule ("R", "L");
	//
	addRule ("S",
		 oneOf (sequence (AT, "S", LEFT_CURLY),
			"B"));
	addRule ("B",
		 oneOf (sequence (LEFT_BRACKET, "B", LEFT_CURLY),
			"C"));
	addRule ("C",
		 oneOf (sequence (LEFT_PARENTHESIS, "C", LEFT_CURLY),
			RIGHT_BRACKET));
	//
	addRule ("S", "A", "B", LEFT_PARENTHESIS);
	addRule ("A", zeroOrOne (AT));
	addRule ("B", zeroOrOne (LEFT_BRACKET));
	*/
    }
}