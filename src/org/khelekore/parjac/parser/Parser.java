package org.khelekore.parjac.parser;

import java.nio.file.Path;
import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.SyntaxTree;

/** A recursive descent parser for java */
public class Parser {
    private final Path path;
    private final Lexer lexer;
    private final CompilerDiagnosticCollector diagnostics;

    private Token nextToken;
    private Token currentToken;

    public Parser (Path path, Lexer lexer, CompilerDiagnosticCollector diagnostics) {
	this.path = path;
	this.lexer = lexer;
	nextToken = lexer.nextNonWhitespaceToken ();
	this.diagnostics = diagnostics;
    }

    public SyntaxTree parse () {
	compilationUnit ();
	Token t = nextToken ();
	if (t == Token.SUB)
	    t = nextToken ();
	if (t != Token.END_OF_INPUT)
	    addParserError ("Extra tokens found: " + t);
	return new SyntaxTree (path); // quite broken
    }

    public CompilerDiagnosticCollector getDiagnostics () {
	return diagnostics;
    }

    private void compilationUnit () {
	Token next = nextToken ();
	while (next == Token.AT) {
	    annotation ();
	    next = nextToken ();
	}
	if (next == Token.PACKAGE) {
	    packageDeclaration ();
	    next = nextToken ();
	}

	while (next == Token.IMPORT) {
	    importDeclaration ();
	    next = nextToken ();
	}

	while (isTypeDeclrationFirst (next)) {
	    typeDeclaration ();
	    next = nextToken ();
	}

	/*
	default:
	    addParserError ("Expected package, import or type declaration, found: " + nextToken);
	    break;
	}
	*/
    }

    private void annotation () {
	match (Token.AT);
	typeName ();
	if (nextToken () == Token.LEFT_PARANTHESIS) {
	    match (Token.LEFT_PARANTHESIS);
	    // TODO: correct parsing
	    while (nextToken () != Token.RIGHT_PARANTHESIS) {
		match (nextToken ());
	    }
	    match (Token.RIGHT_PARANTHESIS);
	}
    }

    private void packageDeclaration () {
	match (Token.PACKAGE);
	match (Token.IDENTIFIER);
	while (nextToken () == Token.DOT) {
	    match (Token.DOT);
	    match (Token.IDENTIFIER);
	}
	match (Token.SEMICOLON);
    }

    private void importDeclaration () {
	match (Token.IMPORT);
	if (nextToken == Token.STATIC)
	    match (Token.STATIC);
	match (Token.IDENTIFIER);
	while (nextToken () == Token.DOT) {
	    match (Token.DOT);
	    if (nextToken () == Token.MULTIPLY) {
		match (Token.MULTIPLY);
		break;
	    } else {
		match (Token.IDENTIFIER);
	    }
	}
	match (Token.SEMICOLON);
    }

    private boolean isTypeDeclrationFirst (Token t) {
	switch (t) {
	case PUBLIC:
	case PROTECTED:
	case PRIVATE:
	case ABSTRACT:
	case STATIC:
	case FINAL:
	case STRICTFP:
	case CLASS:
	case ENUM:
	case INTERFACE:
	    return true;
	default:
	    return false;
	}
    }

    private void typeDeclaration () {
	// TODO: fill in
    }

    private void typeName () {
	match (Token.IDENTIFIER);
	while (nextToken () == Token.DOT) {
	    match (Token.DOT);
	    match (Token.IDENTIFIER);
	}
    }

    private void match (Token t) {
	if (t != nextToken) {
	    addParserError ("Got: " + currentToken + ", expected: " + t);
	    // TODO: better error handling
	}
	nextToken = lexer.nextNonWhitespaceToken ();
    }

    private Token nextToken () {
	return nextToken;
    }

    private void checkAllTokens () {
	while (lexer.hasMoreTokens ()) {
	    Token t = lexer.nextToken ();
	    if (t == Token.ERROR)
		addLexerError ();
	}
    }

    private void addLexerError () {
	addParserError (lexer.getError ());
    }

    private void addParserError (String error) {
	diagnostics.report (new SourceDiagnostics (path,
						   lexer.getTokenStartPos (),
						   lexer.getTokenEndPos (),
						   lexer.getLine (),
						   lexer.getTokenColumn (),
						   error));
    }
}
