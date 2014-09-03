package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.util.EnumSet;
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
	Token next;
	while (nextToken () == Token.AT) {
	    annotation ();
	}
	if (nextToken () == Token.PACKAGE) {
	    packageDeclaration ();
	}

	while (nextToken () == Token.IMPORT) {
	    // TODO: if we have annotations we need to error
	    importDeclaration ();
	}

	while (typeDeclarationFirsts.contains (nextToken ())) {
	    typeDeclaration ();
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
	if (nextToken () == Token.LEFT_PARENTHESIS) {
	    match (Token.LEFT_PARENTHESIS);
	    // TODO: correct parsing
	    while (nextToken () != Token.RIGHT_PARENTHESIS) {
		match (nextToken ());
	    }
	    match (Token.RIGHT_PARENTHESIS);
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

    private static final EnumSet<Token> typeDeclarationFirsts = EnumSet.of (
	Token.PUBLIC, Token.PROTECTED, Token.PRIVATE, Token.ABSTRACT, Token.STATIC, Token.FINAL,
	Token.STRICTFP, Token.CLASS, Token.ENUM, Token.INTERFACE, Token.SEMICOLON);

    /*
      TypeDeclaration:
          ClassDeclaration
	  InterfaceDeclaration
	  ;

      ClassDeclaration:
          NormalClassDeclaration
	  EnumDeclaration

      NormalClassDeclaration:
          {ClassModifier} class Identifier [TypeParameters] [Superclass] [Superinterfaces] ClassBody

      EnumDeclaration:
          {ClassModifier} enum Identifier [Superinterfaces] EnumBody

      InterfaceDeclaration:
          NormalInterfaceDeclaration
	  AnnotationTypeDeclaration

      NormalInterfaceDeclaration:
          {InterfaceModifier} interface Identifier [TypeParameters] [ExtendsInterfaces] InterfaceBody

      AnnotationTypeDeclaration:
          {InterfaceModifier} @ interface Identifier AnnotationTypeBody
    */
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
						   lexer.getLineNumber (),
						   lexer.getTokenColumn (),
						   error));
    }
}
