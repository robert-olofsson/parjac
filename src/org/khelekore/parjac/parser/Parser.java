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
     */
    private void compilationUnit () {
	packageDeclaration ();
	importDeclarations ();
	typeDeclarations ();

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

      PackageModifier:
          Annotation
    */
    private void packageDeclaration () {
	annotations ();
	if (nextToken () == Token.PACKAGE) {
	    match (Token.PACKAGE);
	    match (Token.IDENTIFIER);
	    while (nextToken () == Token.DOT) {
		match (Token.DOT);
		match (Token.IDENTIFIER);
	    }
	    match (Token.SEMICOLON);
	}
    }

    /*
      ImportDeclaration:
          SingleTypeImportDeclaration
	  TypeImportOnDemandDeclaration
	  SingleStaticImportDeclaration
	  StaticImportOnDemandDeclaration
    */
    private void importDeclarations () {
	while (nextToken () == Token.IMPORT) {
	    importDeclaration ();
	}
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
	Token.STRICTFP, Token.CLASS, Token.ENUM, Token.INTERFACE, Token.AT, Token.SEMICOLON);

    private void typeDeclarations () {
	while (typeDeclarationFirsts.contains (nextToken ())) {
	    typeDeclaration ();
	}
    }

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
	default:
	    addParserError ("Expected typeDeclaration, got: " + nextToken ());
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

    private void interfaceType () {
	classType ();
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

    private void typeName () {
	match (Token.IDENTIFIER);
	while (nextToken () == Token.DOT) {
	    match (Token.DOT);
	    match (Token.IDENTIFIER);
	}
    }

    /* Productions from §4 (Types, Values, and Variables), in same order */
    private static final EnumSet<Token> primitiveTypes = EnumSet.of (
	Token.BYTE, Token.SHORT, Token.INT, Token.LONG, Token.CHAR, Token.FLOAT, Token.DOUBLE);

    private void type () {
	if (primitiveTypes.contains (nextToken ()))
	    primitiveType ();
	else
	    referenceType ();
    }

    private void primitiveType () {
	annotations ();
	if (nextToken() == Token.BOOLEAN)
	    match (Token.BOOLEAN);
	else
	    NumericType ();
    }

    private static final EnumSet<Token> integralTypes = EnumSet.of (
	Token.BYTE, Token.SHORT, Token.INT, Token.LONG, Token.CHAR);

    private void NumericType () {
	if (integralTypes.contains (nextToken ()))
	    integralType ();
	else
	    floatingPointType ();
    }

    private void integralType () {
	switch (nextToken ()) {
	case BYTE:
	case SHORT:
	case INT:
	case LONG:
	case CHAR:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected integral type, found: " + nextToken ());
	}
    }

    private void floatingPointType () {
	switch (nextToken ()) {
	case FLOAT:
	case DOUBLE:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected floating point type, found: " + nextToken ());
	}
    }

    private void referenceType () {
	// TODO: one of
	{
	    classOrInterfaceType ();
	}
	{
	    typeVariable ();
	}
	{
	    arrayType ();
	}
    }

    private void classOrInterfaceType () {
	classType ();
	interfaceType ();
    }

    private void classType () {
	// TODO: one of
	{
	    annotations ();
	    match (Token.IDENTIFIER);
	    if (nextToken () == Token.LT)
		typeArgument ();
	}
	{
	    classOrInterfaceType ();
	    match (Token.DOT);
	    annotations ();
	    match (Token.IDENTIFIER);
	    if (nextToken () == Token.LT)
		typeArgument ();
	}
    }

    private void typeVariable () {
	annotations ();
	match (Token.IDENTIFIER);
    }

    private void arrayType () {
	// TODO: one of
	{
	    primitiveType ();
	    dims ();
	}
	{
	    classOrInterfaceType ();
	    dims ();
	}
	{
	    typeVariable ();
	    dims ();
	}
    }

    private void dims () {
	annotations ();
	match (Token.LEFT_BRACKET);
	match (Token.RIGHT_BRACKET);
	// TODO: one or more: {Annotation} [ ]
    }

    private void typeParameter () {
	annotations ();
	match (Token.IDENTIFIER);
	if (nextToken () == Token.EXTENDS)
	    typeBound ();
    }

    private void typeBound () {
	match (Token.EXTENDS);
	// TODO: one of
	{
	    typeVariable ();
	}
	{
	    classOrInterfaceType ();
	    while (nextToken () == Token.BIT_AND) {
		additionalBound ();
	    }
	}
    }

    private void additionalBound () {
	match (Token.BIT_AND);
	interfaceType ();
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
	// TODO: one of
	{
	    referenceType ();
	}
	{
	    wildcard ();
	}
    }

    private void wildcard () {
	annotations();
	match (Token.QUESTIONMARK);
	if (nextToken() == Token.EXTENDS || nextToken() == Token.SUPER)
	    wildcardBounds ();
    }

    private void wildcardBounds () {
	switch (nextToken ()) {
	case EXTENDS:
	    match (Token.EXTENDS);
	    referenceType ();
	    break;
	case SUPER:
	    match (Token.SUPER);
	    referenceType ();
	    break;
	default:
	    addParserError ("Expected super or extends, found: " + nextToken ());
	}
    }
    /* End of §4 */

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

    /*
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
    */

    private void addParserError (String error) {
	diagnostics.report (new SourceDiagnostics (path,
						   lexer.getTokenStartPos (),
						   lexer.getTokenEndPos (),
						   lexer.getLineNumber (),
						   lexer.getTokenColumn (),
						   error));
    }
}
