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

    /*
      CompilationUnit:
          [PackageDeclaration] {ImportDeclaration} {TypeDeclaration}

      PackageDeclaration:
          {PackageModifier} package Identifier {. Identifier} ;

      PackageModifier:
          Annotation
     */
    private void compilationUnit () {
	Token next;

	annotations ();
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

    /*
      PackageDeclaration:
          {PackageModifier} package Identifier {. Identifier} ;
    */
    private void packageDeclaration () {
	match (Token.PACKAGE);
	match (Token.IDENTIFIER);
	while (nextToken () == Token.DOT) {
	    match (Token.DOT);
	    match (Token.IDENTIFIER);
	}
	match (Token.SEMICOLON);
    }

    /*
      ImportDeclaration:
          SingleTypeImportDeclaration
	  TypeImportOnDemandDeclaration
	  SingleStaticImportDeclaration
	  StaticImportOnDemandDeclaration
    */
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
	Token.STRICTFP, Token.CLASS, Token.ENUM, Token.INTERFACE, Token.AT, Token.SEMICOLON);

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
	if (nextToken () == Token.SEMICOLON)
	    return;

	modifiers ();
	switch (nextToken ()) {
	case CLASS:
	    normalClassDeclaration ();
	    break;
	case ENUM:
	    enumDeclaration ();
	    break;
	case INTERFACE:
	    normalInterfaceDeclaration ();
	    break;
	case AT:
	    annotationTypeDeclaration ();
	    break;
	}
    }

    private void modifiers () {
	do {
	    if (nextToken () == Token.AT)
		annotation ();
	    else if (modifiers.contains (nextToken ()))
		match (nextToken ());
	} while (nextToken () == Token.AT || modifiers.contains (nextToken ()));
    }

    // TODO: FINAL may not be used for interfaces
    private static final EnumSet<Token> modifiers = EnumSet.of (
	Token.PUBLIC, Token.PROTECTED, Token.PRIVATE, Token.ABSTRACT,
	Token.STATIC, Token.FINAL, Token.STRICTFP);

    private void normalClassDeclaration () {
	match (Token.CLASS);
	match (Token.IDENTIFIER);
	if (nextToken () == Token.LT)
	    typeParameters ();
	if (nextToken () == Token.SUPER)
	    superClass ();
	if (nextToken () == Token.IMPLEMENTS)
	    superInterfaces ();
	classBody ();
    }

    private void enumDeclaration () {
	match (Token.ENUM);
	match (Token.IDENTIFIER);
	if (nextToken () == Token.IMPLEMENTS)
	    superInterfaces ();
	enumBody ();
    }

    private void normalInterfaceDeclaration () {
	match (Token.INTERFACE);
	match (Token.IDENTIFIER);
	if (nextToken () == Token.LT)
	    typeParameters ();
	if (nextToken () == Token.EXTENDS)
	    extendsInterfaces ();
	interfaceBody ();
    }

    private void annotationTypeDeclaration () {
	match (Token.AT);
	match (Token.INTERFACE);
	match (Token.IDENTIFIER);
	annotationTypeBody ();
    }

    private void typeParameters () {
	match (Token.LT);
	typeParameterList ();
	match (Token.GT);
    }

    private void typeParameterList () {
	typeParameter ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    typeParameter ();
	}
    }

    private void typeParameter () {
	annotations ();
	match (Token.IDENTIFIER);
	if (nextToken () == Token.EXTENDS)
	    typeBound ();
    }

    private void typeBound () {
	match (Token.EXTENDS);
	annotations ();
	// TODO: this is not complete
	match (Token.IDENTIFIER);
	if (nextToken () == Token.LT)
	    typeArguments ();
	while (nextToken () == Token.BIT_AND) {
	    match (Token.BIT_AND);
	    interfaceType ();
	}
    }

    private void interfaceType () {
	classType ();
    }

    private void classType () {
    }

    private void typeArguments () {
	match (Token.LT);
	typeArgumentList ();
	match (Token.GT);
    }

    private void typeArgumentList () {
	typeArgument ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    typeArgument ();
	}
    }

    private void typeArgument () {
	// ReferenceType
	// Wildcard
    }

    private void superClass () {
	match (Token.EXTENDS);
	classType ();
    }

    private void superInterfaces () {
	match (Token.IMPLEMENTS);
	interfaceTypeList ();
    }

    private void interfaceTypeList () {
	interfaceType ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    interfaceType ();
	}
    }

    private void classBody () {
	match (Token.LEFT_CURLY);
	// TODO
	match (Token.RIGHT_CURLY);
    }

    private void enumBody () {
	match (Token.LEFT_CURLY);
	// TODO
	match (Token.RIGHT_CURLY);
    }

    private void extendsInterfaces () {
	match (Token.EXTENDS);
	interfaceTypeList ();
    }

    private void interfaceBody () {
	match (Token.LEFT_CURLY);
	// TODO
	match (Token.RIGHT_CURLY);
    }

    private void annotationTypeBody () {
	match (Token.LEFT_CURLY);
	// TODO
	match (Token.RIGHT_CURLY);
    }

    private void annotations () {
	while (nextToken () == Token.AT)
	    annotation ();
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

    private void typeName () {
	match (Token.IDENTIFIER);
	while (nextToken () == Token.DOT) {
	    match (Token.DOT);
	    match (Token.IDENTIFIER);
	}
    }

    private void match (Token t) {
	if (t != nextToken) {
	    addParserError ("nextToken: " + nextToken + ", expected: " + t);
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
