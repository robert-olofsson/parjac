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
import org.khelekore.parjac.lexer.CharBufferLexer;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.parser.LRParser;
import org.khelekore.parjac.parser.Parser;

import static org.khelekore.parjac.lexer.Token.*;

public class Trivial {
    private final LRParser lr;

    public Trivial () {
	lr = new LRParser ();
	addRules ();
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
	    Lexer lexer = new CharBufferLexer (path, charBuf);
	    Parser p = new Parser (g.lr, path, lexer, diagnostics);
	    p.parse ();
	    diagnostics.getDiagnostics ().
		forEach (d -> System.err.println (d.getMessage (locale)));
	}
    }

    private void addRules () {
	lr.addRule ("Goal", "S");
	lr.addRule ("S", "E", EQUAL, "E");
	lr.addRule ("S", IDENTIFIER);
	lr.addRule ("E", "E", PLUS, IDENTIFIER);
	lr.addRule ("E", IDENTIFIER);

	/*
	lr.addRule ("Goal", "E");
	lr.addRule ("E", "T", PLUS, "E");
	lr.addRule ("E", "T");
	lr.addRule ("T", IDENTIFIER);

	lr.addRule ("Goal", "E");
	lr.addRule ("E", "E", PLUS, "T");
	lr.addRule ("E", "E", MINUS, "T");
	lr.addRule ("E", "T");
	lr.addRule ("T", "T", MULTIPLY, "F");
	lr.addRule ("T", "T", DIVIDE, "F");
	lr.addRule ("T", "F");
	lr.addRule ("F", LEFT_PARENTHESIS, "E", RIGHT_PARENTHESIS);
	lr.addRule ("F", IDENTIFIER);

	lr.addRule ("Goal", "E", END_OF_INPUT);
	lr.addRule ("E", "E", PLUS, "T");
	lr.addRule ("E", "T");
	lr.addRule ("T", "T", MULTIPLY, "P");
	lr.addRule ("T", "P");
	lr.addRule ("P", IDENTIFIER);
	lr.addRule ("P", LEFT_PARENTHESIS, "E", RIGHT_PARENTHESIS);

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