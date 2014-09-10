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

    /* Productions from §4 (Types, Values, and Variables), in same order */

    private static final EnumSet<Token> primitiveTypes = EnumSet.of (
	Token.BOOLEAN,
	Token.BYTE, Token.SHORT, Token.INT, Token.LONG, Token.CHAR,
	Token.FLOAT, Token.DOUBLE);

    private void primitiveType () {
	annotations ();
	unannPrimitiveType ();
    }

    private void numericType () {
	switch (nextToken ()) {
	case BYTE:
	case SHORT:
	case INT:
	case LONG:
	case CHAR:
 	    match (nextToken ());
	    break;
	case DOUBLE:
	case FLOAT:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected integral type, found: " + nextToken ());
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
	// classType and interfaceType are the same
	classType ();
    }

    private void classType () {
	annotations ();
	unannClassType ();
    }

    private void interfaceType () {
	classType ();
    }

    private void typeVariable () {
	annotations ();
	match (Token.IDENTIFIER);
    }

    private void arrayType () {
	annotations ();
	unannArrayType ();
	dims ();
    }

    private void zeroOrOneDims () {
	if (dimsFirst.contains (nextToken ()))
	    dims ();
    }

    private static final EnumSet<Token> dimsFirst = EnumSet.of (
	Token.AT, Token.LEFT_BRACKET);

    private void dims () {
	annotations ();
	match (Token.LEFT_BRACKET);
	match (Token.RIGHT_BRACKET);
	while (nextToken () == Token.AT || nextToken () == Token.LEFT_BRACKET) {
	    annotations ();
	    match (Token.LEFT_BRACKET);
	    match (Token.RIGHT_BRACKET);
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
	// TODO: one of
	{   // IDENTIFIER
	    unannTypeVariable ();
	}
	{   // IDENTIFIER[typearguments]{.IDENTIFIER[typearguments]}
	    unannClassOrInterfaceType ();
	    additionalBounds ();
	}
    }

    private void additionalBounds () {
	while (nextToken () == Token.BIT_AND) {
	    additionalBound ();
	}
    }

    private void additionalBound () {
	match (Token.BIT_AND);
	interfaceType ();
    }

    private void zeroOrOneTypeArguments () {
	if (nextToken () == Token.LT)
	    typeArguments ();
    }

    private void typeArguments () {
	match (Token.LT);
	lexer.setInsideTypeContext (true);
	typeArgumentList ();
	match (Token.GT);
	lexer.setInsideTypeContext (false);
    }

    private void typeArgumentList () {
	typeArgument ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    typeArgument ();
	}
    }

    private void typeArgument () {
	annotations ();
	if (nextToken () == Token.QUESTIONMARK)
	    wildcard ();
	else
	    unannReferenceType ();
    }

    private void wildcard () {
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

    /* Productions from §6 Names*/

    private void typeName () {
	identifiersConnectedByDots();
    }

    private void expressionName () {
	identifiersConnectedByDots();
    }

    private void methodName () {
	match (Token.IDENTIFIER);
    }

    private void identifiersConnectedByDots () {
	match (Token.IDENTIFIER);
	while (nextToken () == Token.DOT) {
	    match (Token.DOT);
	    match (Token.IDENTIFIER);
	}
    }

    /* End of §6 */

    /* Productions from §7 (Packages) */
    private void compilationUnit () {
	packageDeclaration ();
	importDeclarations ();
	typeDeclarations ();
    }

    private void packageDeclaration () {
	packageModifiers ();
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

    private void packageModifiers () {
	annotations ();
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
	if (nextToken () == Token.STATIC)
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

    private void typeDeclarations () {
	while (typeDeclarationFirsts.contains (nextToken ())) {
	    typeDeclaration ();
	}
    }

    private static final EnumSet<Token> typeDeclarationFirsts = EnumSet.of (
	Token.PUBLIC, Token.PROTECTED, Token.PRIVATE, Token.ABSTRACT, Token.STATIC, Token.FINAL,
	Token.STRICTFP, Token.CLASS, Token.ENUM, Token.INTERFACE, Token.AT, Token.SEMICOLON);

    private void typeDeclaration () {
	if (nextToken () == Token.SEMICOLON) {
	    match (Token.SEMICOLON);
	    return;
	}

	classModifiers ();
	switch (nextToken ()) {
	case CLASS:
	    normalClassDeclaration ();
	    break;
	case ENUM:
	    enumDeclaration ();
	    break;
	case INTERFACE:
	    // TODO: check final modifier
	    normalInterfaceDeclaration ();
	    break;
	case AT:
	    // TODO: check final modifier
	    annotationTypeDeclaration ();
	    break;
	default:
	    addParserError ("Expected typeDeclaration, got: " + nextToken ());
	    break;
	}
    }

    /* End of §7 */

    /* Productions from §8 (Classes) */

    private void classDeclaration () {
	classModifiers ();
	if (nextToken () == Token.CLASS)
	    normalClassDeclaration ();
	else if (nextToken () == Token.ENUM)
	    enumDeclaration ();
	else
	    addParserError ("Expected class or enum, found: " + nextToken ());
    }

    private void normalClassDeclaration () {
	match (Token.CLASS);
	match (Token.IDENTIFIER);
	zeroOrOneTypeParameters ();
	if (nextToken () == Token.SUPER)
	    superClass ();
	if (nextToken () == Token.IMPLEMENTS)
	    superInterfaces ();
	classBody ();
    }

    private void classModifiers () {
	while (classModifersFirst.contains (nextToken ())) {
	    classModifier ();
	}
    }

    private static final EnumSet<Token> classModifersFirst = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.PROTECTED, Token.PRIVATE, Token.ABSTRACT,
	Token.STATIC, Token.FINAL, Token.STRICTFP);

    private void classModifier () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case PUBLIC:
	case PROTECTED:
	case PRIVATE:
	case ABSTRACT:
	case STATIC:
	case FINAL:
	case STRICTFP:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected class modifier, found: " + nextToken ());
	}
    }

    private void zeroOrOneTypeParameters () {
	if (nextToken () == Token.LT)
	    typeParameters ();
    }

    private void typeParameters () {
	match (Token.LT);
	lexer.setInsideTypeContext (true);
	typeParameterList ();
	match (Token.GT);
	lexer.setInsideTypeContext (false);
    }

    private void typeParameterList () {
	typeParameter ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    typeParameter ();
	}
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
	classBodyDeclarations ();
	match (Token.RIGHT_CURLY);
    }

    private void classBodyDeclarations () {
	while (classBodyDeclarationFirsts.contains (nextToken ()))
	    classBodyDeclaration ();
    }

    private EnumSet<Token> classBodyDeclarationFirsts = EnumSet.of (
	/* annotations */
	Token.AT,
	/* field declarations */
	Token.PUBLIC, Token.PROTECTED, Token.PRIVATE, Token.STATIC,
	Token.FINAL, Token.TRANSIENT, Token.VOLATILE,
	/* method declarations */
	Token.ABSTRACT, Token.SYNCHRONIZED, Token.NATIVE, Token.STRICTFP,
	/* return types */
	Token.VOID, Token.BOOLEAN,
	Token.BYTE, Token.SHORT, Token.INT, Token.LONG, Token.CHAR,
	Token.FLOAT, Token.DOUBLE,
	/* classes */
	Token.CLASS,
	Token.INTERFACE,
	/* empty */
	Token.SEMICOLON,
	/* initializers */
	Token.LEFT_CURLY, Token.STATIC );

    private void classBodyDeclaration () {
	// TODO: one of
	{
	    classMemberDeclaration ();
	}
	{ // {
	    instanceInitializer ();
	}
	{ // static { however static may also be used for field, method, class
	    staticInitializer ();
	}
	{ // constructorModifiersFirst or IDENTIFIER
	    constructorDeclaration ();
	}
    }

    private void classMemberDeclaration () {
	if (nextToken () == Token.SEMICOLON) {
	    match (Token.SEMICOLON);
	} else {
	    // TODO: one of
	    {
		fieldDeclaration ();
	    }
	    {
		methodDeclaration ();
	    }
	    {
		classDeclaration ();
	    }
	    {
		interfaceDeclaration ();
	    }
	}
    }

    private void fieldDeclaration () {
	fieldModifiers ();
	unannType ();
	variableDeclaratorList ();
	match (Token.SEMICOLON);
    }

    private void fieldModifiers () {
	while (fieldModifiersFirst.contains (nextToken ()))
	    fieldModifier ();
    }

    private static final EnumSet<Token> fieldModifiersFirst = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.PROTECTED, Token.PRIVATE, Token.STATIC,
	Token.FINAL, Token.TRANSIENT, Token.VOLATILE);

    private void fieldModifier () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case PUBLIC:
	case PROTECTED:
	case PRIVATE:
	case STATIC:
	case FINAL:
	case TRANSIENT:
	case VOLATILE:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected field modifier, found: " + nextToken ());
	}
    }

    private void variableDeclaratorList () {
	variableDeclarator ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    variableDeclarator ();
	}
    }

    private void variableDeclarator () {
	variableDeclaratorId ();
	if (nextToken () == Token.EQUAL) {
	    match (Token.EQUAL);
	    variableInitializer ();
	}
    }

    private void variableDeclaratorId () {
	match (Token.IDENTIFIER);
	zeroOrOneDims ();
    }

    private void variableInitializer () {
	// TODO: one of
	{
	    expression ();
	}
	{
	    arrayInitializer ();
	}
    }

    private void unannType () {
	if (primitiveTypes.contains (nextToken ()))
	    unannPrimitiveType ();
	else
	    unannReferenceType ();
    }

    private void unannPrimitiveType () {
	if (nextToken () == Token.BOOLEAN)
	    match (Token.BOOLEAN);
	else
	    numericType ();
    }

    private void unannReferenceType () {
	// TODO: one of
	{
	    unannClassOrInterfaceType ();
	}
	{
	    unannTypeVariable ();
	}
	{
	    unannArrayType ();
	}
    }

    private void unannClassOrInterfaceType () {
	// unannClassType or unannInterfaceType, but they are the same
	unannClassType ();
    }

    private void unannClassType () {
	match (Token.IDENTIFIER);
	zeroOrOneTypeArguments ();
	while (nextToken () == Token.DOT) {
	    match (Token.DOT);
	    annotations ();
	    match (Token.IDENTIFIER);
	    zeroOrOneTypeArguments ();
	}
    }

    private void unannTypeVariable () {
	match (Token.IDENTIFIER);
    }

    private void unannArrayType () {
	if (primitiveTypes.contains (nextToken ())) {
	    unannPrimitiveType ();
	} else {
	    // unannClassOrInterfaceType () or unannTypeVariable
	    // if it has DOT in it it is a classType, otherwise we are not sure
	    unannClassType ();
	}
	dims ();
    }

    private void methodDeclaration () {
	methodModifiers ();
	methodHeader ();
	methodBody ();
    }

    private void methodModifiers () {
	while (methodModifiersFirst.contains (nextToken ()))
	    methodModifier ();
    }

    private static final EnumSet<Token> methodModifiersFirst = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.PROTECTED, Token.PRIVATE, Token.ABSTRACT,
	Token.STATIC, Token.FINAL, Token.SYNCHRONIZED, Token.NATIVE, Token.STRICTFP);

    private void methodModifier () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case PUBLIC:
	case PROTECTED:
	case PRIVATE:
	case ABSTRACT:
	case STATIC:
	case FINAL:
	case SYNCHRONIZED:
	case NATIVE:
	case STRICTFP:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected method modifier, found: " + nextToken ());
	}
    }

    private void methodHeader () {
	// TODO: one of
	{
	    // empty
	}
	{
	    typeParameters ();
	    annotations ();
	}
	result ();
	methodDeclarator ();
	zeroOrOneThrowsClause ();
    }

    private void result () {
	if (nextToken () == Token.VOID) {
	    match (Token.VOID);
	} else {
	    unannType ();
	}
    }

    private void methodDeclarator () {
	match (Token.IDENTIFIER);
	match (Token.LEFT_PARENTHESIS);
	zeroOrOneFormalParmeterList ();
	match (Token.RIGHT_PARENTHESIS);
	zeroOrOneDims ();
    }

    private void zeroOrOneFormalParmeterList () {
	if (nextToken () != Token.RIGHT_PARENTHESIS)
	    formalParameterList ();
    }

    private void formalParameterList () {
	// TODO: one of
	{
	    formalParameters ();
	    match (Token.COMMA);
	}
	{
	    // empty
	}
	lastFormalParameter ();
    }

    private void formalParameters () {
	// TODO: one of
	{
	    formalParameter ();
	}
	{
	    receiverParameter ();
 	}
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    formalParameter ();
	}
    }

    private void formalParameter () {
	variableModifiers ();
	unannType ();
	variableDeclaratorId ();
    }

    private void variableModifiers () {
	while (variableModifiersFirst.contains (nextToken()))
	    variableModifier ();
    }

    private EnumSet<Token> variableModifiersFirst = EnumSet.of (
	Token.AT, Token.FINAL);

    private void variableModifier () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case FINAL:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected variable modifer, found: " + nextToken ());
	}
    }

    private void lastFormalParameter () {
	// TODO: one of
	{
	    variableModifiers ();
	    unannType ();
	    annotations ();
	    match (Token.ELLIPSIS);
	    variableDeclaratorId ();
	}
	{
	    formalParameter ();
	}
    }

    private void receiverParameter () {
	annotations ();
	unannType ();
	if (nextToken () == Token.IDENTIFIER) {
	    match (Token.IDENTIFIER);
	    match (Token.DOT);
	}
	match (Token.THIS);
    }

    private void zeroOrOneThrowsClause () {
	if (nextToken () == Token.THROW)
	    throwsClause ();
    }

    private void throwsClause () {
	match (Token.THROW);
	exceptionTypeList ();
    }

    private void exceptionTypeList () {
	exceptionType ();
	while (nextToken() == Token.COMMA) {
	    match (Token.COMMA);
	    exceptionType ();
	}
    }

    private void exceptionType () {
	// TODO: one of
	{
	    classType ();
	}
	{
	    typeVariable ();
	}
    }

    private void methodBody () {
	if (nextToken () == Token.SEMICOLON)
	    match (Token.SEMICOLON);
	else
	    block ();
    }

    private void instanceInitializer () {
	block ();
    }

    private void staticInitializer () {
	match (Token.STATIC);
	block ();
    }

    private void constructorDeclaration () {
	constructorModifiers ();
	constructorDeclarator ();
	zeroOrOneThrowsClause ();
	constructorBody ();
    }

    private void constructorModifiers () {
	while (constructorModifiersFirst.contains (nextToken ()))
	    constructorModifier ();
    }

    private static final EnumSet<Token> constructorModifiersFirst = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.PROTECTED, Token.PRIVATE);

    private void constructorModifier () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case PUBLIC:
	case PROTECTED:
	case PRIVATE:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected variable modifer, found: " + nextToken ());
	}
    }

    private void constructorDeclarator () {
	zeroOrOneTypeParameters ();
	simpleTypeName ();
	match (Token.LEFT_PARENTHESIS);
	zeroOrOneFormalParmeterList ();
	match (Token.RIGHT_PARENTHESIS);
    }

    private void simpleTypeName () {
	match (Token.IDENTIFIER);
    }

    private void constructorBody () {
	match (Token.LEFT_CURLY);
	explicitConstructorInvocation (); // TODO: 0-1
	blockStatements ();
	match (Token.RIGHT_CURLY);
    }

    private void explicitConstructorInvocation () {
	// TODO: one of
	{
	    zeroOrOneTypeArguments ();
	    match (Token.THIS);
	}
	{
	    zeroOrOneTypeArguments ();
	    match (Token.SUPER);
	}
	{
	    expressionName ();
	    match (Token.DOT);
	    zeroOrOneTypeArguments ();
	    match (Token.SUPER);
	}
	{
	    primary ();
	    match (Token.DOT);
	    zeroOrOneTypeArguments ();
	    match (Token.SUPER);
	}

	// always followed by this
	match (Token.LEFT_PARENTHESIS);
	if (nextToken() != Token.RIGHT_PARENTHESIS)
	    argumentList ();
	match (Token.RIGHT_PARENTHESIS);
	match (Token.SEMICOLON);
    }

    private void enumDeclaration () {
	match (Token.ENUM);
	match (Token.IDENTIFIER);
	if (nextToken () == Token.IMPLEMENTS)
	    superInterfaces ();
	enumBody ();
    }

    private void enumBody () {
	match (Token.LEFT_CURLY);
	if (enumConstantListFirsts.contains (nextToken ()))
	    enumConstantList ();
	if (nextToken() == Token.COMMA)
	    match (Token.COMMA);
	if (nextToken() == Token.SEMICOLON)
	    enumBodyDeclarations ();
	match (Token.RIGHT_CURLY);
    }

    private static final EnumSet<Token> enumConstantListFirsts = EnumSet.of (
	Token.AT, Token.IDENTIFIER);

    private void enumConstantList () {
	enumConstant ();
	while (nextToken() == Token.COMMA) {
	    match (Token.COMMA);
	    enumConstant ();
	}
    }

    private void enumConstant () {
	enumConstantModifiers ();
	match (Token.IDENTIFIER);
	if (nextToken () == Token.LEFT_PARENTHESIS) {
	    match (Token.LEFT_PARENTHESIS);
	    if (nextToken () != Token.RIGHT_PARENTHESIS)
		argumentList ();
	    match (Token.RIGHT_PARENTHESIS);
	}
	if (nextToken() == Token.LEFT_CURLY)
	    classBody ();
    }

    private void enumConstantModifiers () {
	while (nextToken () == Token.AT)
	    enumConstantModifier ();
    }

    private void enumConstantModifier () {
	annotation ();
    }

    private void enumBodyDeclarations () {
	match (Token.SEMICOLON);
	classBodyDeclarations ();
    }

    /* End of §8 */

    /* Productions from §9 (Interfaces) */

    private void interfaceDeclaration () {
	// TODO: one of
	{
	    normalInterfaceDeclaration ();
	}
	{
	    annotationTypeDeclaration ();
	}
    }

    private void normalInterfaceDeclaration () {
	interfaceModifiers ();
	match (Token.INTERFACE);
	match (Token.IDENTIFIER);
	zeroOrOneTypeParameters ();
	if (nextToken () == Token.EXTENDS)
	    extendsInterfaces ();
	interfaceBody ();
    }

    private void interfaceModifiers () {
	while (interfaceModifiersFirst.contains (nextToken ()))
	    interfaceModifer ();
    }

    private static final EnumSet<Token> interfaceModifiersFirst = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.PROTECTED, Token.PRIVATE, Token.ABSTRACT,
	Token.STATIC, Token.STRICTFP);

    private void interfaceModifer () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case PUBLIC:
	case PROTECTED:
	case PRIVATE:
	case ABSTRACT:
	case STATIC:
	case STRICTFP:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected interface modifier, found: " + nextToken ());
	}
    }

    private void extendsInterfaces () {
	match (Token.EXTENDS);
	interfaceTypeList ();
    }

    private void interfaceBody () {
	match (Token.LEFT_CURLY);
	interfaceMemberDeclarations ();
	match (Token.RIGHT_CURLY);
    }

    private void interfaceMemberDeclarations () {
	while (interfaceMemberDeclarationsFirsts.contains (nextToken ()))
	    interfaceMemberDeclaration ();
    }

    private EnumSet<Token> interfaceMemberDeclarationsFirsts = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.STATIC, Token.FINAL,
	Token.BOOLEAN, Token.BYTE, Token.SHORT, Token.INT, Token.LONG, Token.CHAR,
	Token.FLOAT, Token.DOUBLE
	); // TODO: fill in

    private void interfaceMemberDeclaration () {
	// TODO: one of
	{
	    constantDeclaration ();
	}
	{
	    interfaceMethodDeclaration ();
	}
	{
	    classDeclaration ();
	}
	{
	    interfaceDeclaration ();
	}
	{
	    match (Token.SEMICOLON);
	}
    }

    private void constantDeclaration () {
	constantModifiers ();
	unannType ();
	variableDeclaratorList ();
	match (Token.SEMICOLON);
    }

    private void constantModifiers () {
	while (constantModifiersFirst.contains (nextToken ()))
	    constantModifier ();
    }

    private EnumSet<Token> constantModifiersFirst = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.STATIC, Token.FINAL);

    private void constantModifier () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case PUBLIC:
	case STATIC:
	case FINAL:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected constant modifier, found: " + nextToken ());
	}
    }

    private void interfaceMethodDeclaration () {
	interfaceMethodModifiers ();
	methodHeader ();
	methodBody ();
    }

    private void interfaceMethodModifiers () {
	while (interfaceMethodModifiersFirst.contains (nextToken ()))
	    interfaceMethodModifier ();
    }

    private static final EnumSet<Token> interfaceMethodModifiersFirst = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.ABSTRACT, Token.DEFAULT, Token.STATIC, Token.STRICTFP);

    private void interfaceMethodModifier () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case PUBLIC:
	case ABSTRACT:
	case DEFAULT:
	case STATIC:
	case STRICTFP:
	    match (nextToken ());
	    break;
	default:
	    addParserError ("Expected interface method modifier, found: " + nextToken ());
	}
    }

    private void annotationTypeDeclaration () {
	interfaceModifiers ();
	match (Token.AT);
	match (Token.INTERFACE);
	match (Token.IDENTIFIER);
	annotationTypeBody ();
    }

    private void annotationTypeBody () {
	match (Token.LEFT_CURLY);
	annotationTypeMemberDeclarations ();
	match (Token.RIGHT_CURLY);
    }

    private void annotationTypeMemberDeclarations () {
	// TODO: one of:
	{
	    annotationTypeElementDeclaration ();
	}
	{
	    constantDeclaration ();
	}
	{
	    classDeclaration ();
	}
	{
	    interfaceDeclaration ();
	}
	{
	    match (Token.SEMICOLON);
	}
    }

    private void annotationTypeElementDeclaration () {
	annotationTypeElementModifiers ();
	unannType ();
	match (Token.IDENTIFIER);
	match (Token.LEFT_PARENTHESIS);
	match (Token.RIGHT_PARENTHESIS);
	zeroOrOneDims ();
	if (nextToken () == Token.DEFAULT)
	    defaultValue ();
	match (Token.SEMICOLON);
    }

    private void annotationTypeElementModifiers () {
	while (annotationTypeElementModifiersFirst.contains (nextToken ()))
	    annotationTypeElementModifier ();
    }

    private EnumSet<Token> annotationTypeElementModifiersFirst = EnumSet.of (
	Token.AT, Token.PUBLIC, Token.ABSTRACT);

    private void annotationTypeElementModifier () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case PUBLIC:
	case ABSTRACT:
	    match (nextToken ());
	default:
	    addParserError ("Expected annotation type element modifier, found: " + nextToken ());
	}
    }

    private void defaultValue () {
	match (Token.DEFAULT);
	elementValue ();
    }

    private void annotations () {
	while (nextToken () == Token.AT)
	    annotation ();
    }

    private void annotation () {
	match (Token.AT);
	typeName ();
	if (nextToken() == Token.LEFT_PARENTHESIS) {
	    // normal annotation or single element annotation
	    match (Token.LEFT_PARENTHESIS);
	    // TODO: one of
	    {
		elementValuePairList ();
	    }
	    {
		elementValue ();
	    }

	    match (Token.RIGHT_PARENTHESIS);
	}
    }

    private void elementValuePairList () {
	elementValuePair ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    elementValuePair ();
	}
    }

    private void elementValuePair () {
	match (Token.IDENTIFIER);
	match (Token.EQUAL);
	elementValue ();
    }

    private void elementValue () {
	switch (nextToken ()) {
	case AT:
	    annotation ();
	    break;
	case LEFT_CURLY:
	    elementValueArrayInitializer ();
	    break;
	default:
	    conditionalExpression ();
	    break;
	}
    }

    private void elementValueArrayInitializer () {
	match (Token.LEFT_CURLY);
	elementValueList ();
	match (Token.RIGHT_CURLY);
    }

    private void elementValueList () {
	elementValue ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    if (nextToken () == Token.RIGHT_CURLY)
		break;
	    elementValue ();
	}
    }

    /* End of §9 */

    /* Productions from §10 (Arrays) */

    private void arrayInitializer () {
	match (Token.LEFT_CURLY);
	if (nextToken () != Token.RIGHT_CURLY)
	    variableInitializerList ();
	match (Token.RIGHT_CURLY);
    }

    private void variableInitializerList () {
	variableInitializer ();
	while (nextToken() == Token.COMMA) {
	    match (Token.COMMA);
	    if (nextToken () == Token.RIGHT_CURLY)
		break;
	    variableInitializer ();
	}
    }

    /* End of §10 */

    /* Productions from §14 (Blocks and Statements) */

    private void block () {
	match (Token.LEFT_CURLY);
	blockStatements ();
	match (Token.RIGHT_CURLY);
    }

    private void blockStatements () {
	while (nextToken () != Token.RIGHT_CURLY)
	    blockStatement ();
    }

    private void blockStatement () {
	// TODO: one of
	{
	    localVariableDeclarationStatement ();
	}
	{
	    classDeclaration ();
	}
	{
	    statement ();
	}
    }

    private void localVariableDeclarationStatement () {
	localVariableDeclaration ();
	match (Token.SEMICOLON);
    }

    private void localVariableDeclaration () {
	variableModifier ();
	unannType ();
	variableDeclaratorList ();
    }

    private void statement () {
	// TODO: one of
	{
	    statementWithoutTrailingSubstatement ();
	}
	{
	    // IDENTIFIER
	    labeledStatement ();
	}
	{
	    // IF
	    ifThenStatement ();
	}
	{
	    // IF
	    ifThenElseStatement ();
	}
	{
	    // WHILE
	    whileStatement ();
	}
	{
	    // FOR
	    forStatement ();
	}
    }

    private void statementNoShortIf () {
	// TODO: one of
	{
	    statementWithoutTrailingSubstatement ();
	}
	{  // IDENTIFIER
	    labeledStatementNoShortIf ();
	}
	{  // IF
	    ifThenElseStatementNoShortIf ();
	}
	{  // WHILE
	    whileStatementNoShortIf ();
	}
	{  // FOR
	    forStatementNoShortIf ();
	}
    }

    private void statementWithoutTrailingSubstatement () {
	switch (nextToken ()) {
	case LEFT_CURLY:
	    block ();
	    break;
	case SEMICOLON:
	    emptyStatement ();
	    break;
	case ASSERT:
	    assertStatement ();
	    break;
	case SWITCH:
	    switchStatement ();
	    break;
	case DO:
	    doStatement ();
	    break;
	case BREAK:
	    breakStatement ();
	    break;
	case CONTINUE:
	    continueStatement ();
	    break;
	case RETURN:
	    returnStatement ();
	    break;
	case SYNCHRONIZED:
	    synchronizedStatement ();
	    break;
	case THROW:
	    throwStatement ();
	    break;
	case TRY:
	    tryStatement ();
	    break;
	default:
	    expressionStatement ();
	}
    }

    private void emptyStatement () {
	match (Token.SEMICOLON);
    }

    private void labeledStatement () {
	match (Token.IDENTIFIER);
	match (Token.COLON);
	statement ();
    }

    private void labeledStatementNoShortIf () {
	match (Token.IDENTIFIER);
	match (Token.COLON);
	statementNoShortIf ();
    }

    private void expressionStatement () {
	statementExpression ();
	match (Token.SEMICOLON);
    }

    private void statementExpression () {
	// TODO: one of
	{
	    assignment ();
	}
	{ // ++
	    preIncrementExpression ();
	}
	{ // --
	    preDecrementExpression ();
	}
	{
	    postIncrementExpression ();
	}
	{
	    postDecrementExpression ();
	}
	{
	    methodInvocation ();
	}
	{
	    classInstanceCreationExpression ();
	}
    }

    private void ifThenStatement () {
	ifStart ();
	statement ();
    }

    private void ifThenElseStatement () {
	ifStart ();
	statementNoShortIf ();
	match (Token.ELSE);
	statement ();
    }

    private void ifThenElseStatementNoShortIf () {
	ifStart ();
	statementNoShortIf ();
	match (Token.ELSE);
	statementNoShortIf ();
    }

    private void ifStart () {
	match (Token.IF);
	match (Token.LEFT_PARENTHESIS);
	expression ();
	match (Token.RIGHT_PARENTHESIS);
    }

    private void assertStatement () {
	match (Token.ASSERT);
	expression ();
	if (nextToken() == Token.COLON) {
	    match (Token.COLON);
	    expression ();
	}
	match (Token.SEMICOLON);
    }

    private void switchStatement () {
	match (Token.SWITCH);
	match (Token.LEFT_PARENTHESIS);
	expression ();
	match (Token.RIGHT_PARENTHESIS);
	switchBlock ();
    }

    private void switchBlock () {
	match (Token.LEFT_CURLY);
	// TODO: this may be buggy
	// { {SwitchBlockStatementGroup} {SwitchLabel} }
	while (switchLabelsFirsts.contains (nextToken())) {
	    switchLabel ();
	    if (nextToken () == Token.RIGHT_CURLY)
		break;
	    if (!switchLabelsFirsts.contains (nextToken()))
		blockStatement ();
	}
	match (Token.RIGHT_CURLY);
    }

    private static final EnumSet<Token> switchLabelsFirsts = EnumSet.of (
	Token.CASE, Token.DEFAULT);

    private void switchLabel () {
	switch (nextToken ()) {
	case CASE:
	    match (Token.CASE);
	    // TODO: one of
	    {
		constantExpression ();
	    }
	    {
		enumConstantName ();
	    }
	    match (Token.COLON);
	    break;
	case DEFAULT:
	    match (Token.DEFAULT);
	    match (Token.COLON);
	    break;
	default:
	    addParserError ("Expected switch label, found: " + nextToken ());
	}
    }

    private void enumConstantName () {
	match (Token.IDENTIFIER);
    }

    private void whileStatement () {
	whileStart ();
	statement ();
    }

    private void whileStatementNoShortIf () {
	whileStart ();
	statementNoShortIf ();
    }

    private void whileStart () {
	match (Token.WHILE);
	match (Token.LEFT_PARENTHESIS);
	expression ();
	match (Token.RIGHT_PARENTHESIS);
    }

    private void doStatement () {
	match (Token.DO);
	statement ();
	whileStart ();
	match (Token.SEMICOLON);
    }

    private void forStatement () {
	// TODO: one of
	{
	    basicForStatement ();
	}
	{
	    enhancedForStatement ();
	}
    }

    private void forStatementNoShortIf () {
	// TODO: one of
	{
	    basicForStatementNoShortIf ();
	}
	{
	    enhancedForStatementNoShortIf ();
	}
    }

    private void basicForStatement () {
	forStart ();
	statement ();
    }

    private void basicForStatementNoShortIf () {
	forStart ();
	statementNoShortIf ();
    }

    private void forStart () {
	match (Token.FOR);
	match (Token.LEFT_PARENTHESIS);
	if (nextToken () != Token.SEMICOLON)
	    forInit ();
	match (Token.SEMICOLON);
	if (nextToken () != Token.SEMICOLON)
	    expression ();
	match (Token.SEMICOLON);
	if (nextToken () != Token.RIGHT_PARENTHESIS)
	    forUpdate ();
	match (Token.RIGHT_PARENTHESIS);
    }

    private void forInit () {
	// TODO: one of
	{
	    statementExpressionList ();
	}
	{
	    localVariableDeclaration ();
	}
    }

    private void forUpdate () {
	statementExpressionList ();
    }

    private void statementExpressionList () {
	statementExpression ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    statementExpression ();
	}
    }

    private void enhancedForStatement () {
	enhancedForStart ();
	statement ();
    }

    private void enhancedForStatementNoShortIf () {
	enhancedForStart ();
	statementNoShortIf ();
    }

    private void enhancedForStart () {
	match (Token.FOR);
	match (Token.LEFT_PARENTHESIS);
	variableModifiers ();
	unannType ();
	variableDeclaratorId ();
	match (Token.COLON);
	expression ();
	match (Token.RIGHT_PARENTHESIS);
    }

    private void breakStatement () {
	match (Token.BREAK);
	if (nextToken() == Token.IDENTIFIER)
	    match (Token.IDENTIFIER);
	match (Token.SEMICOLON);
    }

    private void continueStatement () {
	match (Token.CONTINUE);
	if (nextToken() == Token.IDENTIFIER)
	    match (Token.IDENTIFIER);
	match (Token.SEMICOLON);
    }

    private void returnStatement () {
	match (Token.RETURN);
	if (nextToken () != Token.SEMICOLON)
 	    expression ();
	match (Token.SEMICOLON);
    }

    private void throwStatement () {
	match (Token.THROW);
	expression ();
	match (Token.SEMICOLON);
    }

    private void synchronizedStatement () {
	match (Token.SYNCHRONIZED);
	match (Token.LEFT_PARENTHESIS);
	expression ();
	match (Token.RIGHT_PARENTHESIS);
	block ();
    }

    private void tryStatement () {
	match (Token.TRY);
	if (nextToken() == Token.LEFT_PARENTHESIS)
	    resourceSpecification ();
	block ();
	if (nextToken() == Token.CATCH)
	    catches ();
	if (nextToken() == Token.FINALLY)
	    finallyClause ();

	// TODO: without resources need either catch or finally
    }

    private void catches () {
	catchClause ();
	while (nextToken () == Token.CATCH)
	    catchClause ();
    }

    private void catchClause () {
	match (Token.CATCH);
	match (Token.LEFT_PARENTHESIS);
	catchFormalParameter ();
	block ();
    }

    private void catchFormalParameter () {
	variableModifiers ();
	catchType ();
	variableDeclaratorId ();
    }

    private void catchType () {
	unannClassType ();
	while (nextToken() == Token.BIT_OR) {
	    match (nextToken ());
	    classType ();
	}
    }

    private void finallyClause () {
	match (Token.FINALLY);
	block ();
    }

    private void resourceSpecification () {
	match (Token.LEFT_PARENTHESIS);
	resourceList ();
	if (nextToken () == Token.SEMICOLON)
	    match (Token.SEMICOLON);
	match (Token.RIGHT_PARENTHESIS);
    }

    private void resourceList () {
	resource ();
	while (nextToken () == Token.SEMICOLON){
	    match (Token.SEMICOLON);
	    resource ();
	}
    }

    private void resource () {
	variableModifiers ();
	unannType ();
	variableDeclaratorId ();
	match (Token.EQUAL);
	expression ();
    }

    /* End of §14 */

    /* Productions from §15 (Expressions) */

    private void primary () {
	// TODO: one of
	{
	    primaryNoNewArray ();
	}
	{ // new
	    arrayCreationExpression ();
	}
    }

    private void primaryNoNewArray () {
	// TODO: one of
	{
	    if (nextToken ().isLiteral ())
		match (nextToken ());
	}
	{ // IDENTIFIER{.IDENTIFIER}
	    typeName ();
	    while (nextToken() == Token.LEFT_BRACKET) {
		match (Token.LEFT_BRACKET);
		match (Token.RIGHT_BRACKET);
	    }
	    match (Token.DOT);
	    match (Token.CLASS);
	}
	{ // VOID
	    match (Token.VOID);
	    match (Token.DOT);
	    match (Token.CLASS);
	}
	{ // THIS
	    match (Token.THIS);
	}
	{ // IDENTIFIER{.IDENTIFIER}
	    typeName ();
	    match (Token.DOT);
	    match (Token.THIS);
	}
	{ // (
	    match (Token.LEFT_PARENTHESIS);
	    expression ();
	    match (Token.RIGHT_PARENTHESIS);
	}
	{ // .... new
	    classInstanceCreationExpression ();
	}
	{ // primary or super or typename
	    fieldAccess ();
	}
	{
	    arrayAccess ();
	}
	{
	    methodInvocation ();
	}
	{
	    methodReference ();
	}
    }

    private void classInstanceCreationExpression () {
	// TODO: one of
	{
	    // empty
	}
	{
	    expressionName ();
	    match (Token.DOT);
	}
	{
	    primary ();
	    match (Token.DOT);
	}

	match (Token.NEW);
	zeroOrOneTypeArguments ();
	annotations ();
	match (Token.IDENTIFIER);
	if (nextToken () == Token.LT)
	    typeArgumentsOrDiamond ();
	match (Token.LEFT_PARENTHESIS);
	if (nextToken() != Token.RIGHT_PARENTHESIS)
	    argumentList ();
	match (Token.RIGHT_PARENTHESIS);
	if (nextToken() == Token.LEFT_CURLY)
	    classBody ();
    }

    private void typeArgumentsOrDiamond () {
	match (Token.LT);
	lexer.setInsideTypeContext (true);
	if (nextToken () != Token.GT)
	    typeArgumentList ();
	match (Token.GT);
	lexer.setInsideTypeContext (false);
    }

    private void fieldAccess () {
	// TODO: one of
	{
	    primary ();
	}
	{
	    match (Token.SUPER);
	}
	{
	    typeName ();
	    match (Token.DOT);
	    match (Token.SUPER);
	}
	match (Token.DOT);
	match (Token.IDENTIFIER);
    }

    private void arrayAccess () {
	// TODO: one of
	{
	    expressionName ();
	}
	{
	    primaryNoNewArray ();
	}
	expression (); // TODO: 0-1
    }

    private void methodInvocation () {
	// TODO: one of
	{ // IDENTIFIER
	    methodName ();
	}
	{ // IDENTIFIER{.IDENTIFIER}
	    typeName ();
	    dotMethod ();
	}
	{ // IDENTIFIER{.IDENTIFIER}
	    expressionName ();
	    dotMethod ();
	}
	{
	    primary ();
	    dotMethod ();
	}
	{ // SUPER
	    match (Token.SUPER);
	    dotMethod ();
	}
	{ // IDENTIFIER{.IDENTIFIER}
	    typeName ();
	    match (Token.DOT);
	    match (Token.SUPER);
	    dotMethod ();
	}
	match (Token.LEFT_PARENTHESIS);
	if (nextToken () != Token.RIGHT_PARENTHESIS)
	    argumentList ();
	match (Token.RIGHT_PARENTHESIS);
    }

    private void dotMethod () {
	match (Token.DOT);
	zeroOrOneTypeArguments ();
	match (Token.IDENTIFIER);
    }

    private void argumentList () {
	expression ();
	while (nextToken () == Token.COMMA) {
	    match (Token.COMMA);
	    expression ();
	}
    }

    private void methodReference () {
	// TODO: one of
	{  // IDENTIFIER{.IDENTIFIER}
	    expressionName ();
	    match (Token.DOUBLE_COLON);
	    zeroOrOneTypeArguments ();
	    match (Token.IDENTIFIER);
	}
	{
	    referenceType ();
	    match (Token.DOUBLE_COLON);
	    zeroOrOneTypeArguments();
	    match (Token.IDENTIFIER);
	}
	{
	    primary ();
	    match (Token.DOUBLE_COLON);
	    zeroOrOneTypeArguments ();
	    match (Token.IDENTIFIER);
	}
	{  // super
	    match (Token.SUPER);
	    match (Token.DOUBLE_COLON);
	    zeroOrOneTypeArguments ();
	    match (Token.IDENTIFIER);
	}
	{ // IDENTIFIER{.IDENTIFIER}
	    typeName ();
	    match (Token.DOT);
	    match (Token.SUPER);
	    match (Token.DOUBLE_COLON);
	    zeroOrOneTypeArguments ();
	    match (Token.IDENTIFIER);
	}
	{ // annotations IDENTIFIER
	    classType ();
	    match (Token.DOUBLE_COLON);
	    zeroOrOneTypeArguments ();
	    match (Token.NEW);
	}
	{ // annotations ...
	    arrayType ();
	    match (Token.DOUBLE_COLON);
	    match (Token.NEW);
	}
    }

    private void arrayCreationExpression () {
	match (Token.NEW);
	// TODO: one of
	{
	    primitiveType ();
	    dimExprs ();
	    zeroOrOneDims ();
	}
	{
	    classOrInterfaceType ();
	    dimExprs ();
	    zeroOrOneDims ();
	}
	{
	    primitiveType ();
	    dims ();
	    arrayInitializer ();
	}
	{
	    classOrInterfaceType ();
	    dims ();
	    arrayInitializer ();
	}
    }

    private void dimExprs () {
	dimExpr ();
	while (dimExprFirsts.contains (nextToken ()))
	    dimExpr ();
    }

    private static final  EnumSet<Token> dimExprFirsts = EnumSet.of (
	Token.AT, Token.LEFT_BRACKET);


    private void dimExpr () {
	annotations ();
	match (Token.LEFT_BRACKET);
	expression ();
	match (Token.RIGHT_BRACKET);
    }

    private void constantExpression () {
	expression ();
    }

    private void expression () {
	// TODO: one of
	{
	    lambdaExpression ();
	}
	{
	    assignmentExpression ();
	}
    }

    private void lambdaExpression () {
	lambdaParameters ();
	match (Token.ARROW);
	lambdaBody ();
    }

    private void lambdaParameters () {
	if (nextToken () == Token.IDENTIFIER) {
	    match (Token.IDENTIFIER);
	} else {
	    match (Token.LEFT_PARENTHESIS);
	    if (nextToken() != Token.RIGHT_PARENTHESIS) {
		// TODO: one of
		{
		    formalParameterList ();
		}
		{
		    inferredFormalParameterList ();
		}
	    }
	    match (Token.RIGHT_PARENTHESIS);
	}
    }

    private void inferredFormalParameterList () {
	match (Token.IDENTIFIER);
	while (nextToken() == Token.COMMA) {
	    match (Token.COMMA);
	    match (Token.IDENTIFIER);
	}
    }

    private void lambdaBody () {
	if (nextToken () == Token.LEFT_CURLY)
	    block ();
	else
	    expression ();
    }

    private void assignmentExpression () {
	// TODO: one of
	{
	    conditionalExpression ();
	}
	{
	    assignment ();
	}
    }

    private void assignment () {
	leftHandSide ();
	assignmentOperator ();
	expression ();
    }

    private void leftHandSide () {
	// TODO: one of
	{  // identifiersConnectedByDots
	    expressionName ();
	}
	{  // primary or super or typename 
	    fieldAccess ();
	}
	{
	    arrayAccess ();
	}
    }

    private void assignmentOperator () {
	if (nextToken ().isAssignmentOperator ())
	    match (nextToken ());
	else
	    addParserError ("Expected assignment operator, found: " + nextToken ());
    }

    private void conditionalExpression () {
	conditionalOrExpression ();
	if (nextToken () == Token.QUESTIONMARK) {
	    match (Token.QUESTIONMARK);
	    expression ();
	    match (Token.COLON);
	    conditionalExpression ();
	}
    }

    private void conditionalOrExpression () {
	// TODO: one of
	{
	    conditionalAndExpression ();
	}
	{
	    conditionalOrExpression ();
	    match (Token.LOGICAL_OR);
	    conditionalAndExpression ();
	}
    }

    private void conditionalAndExpression () {
	// TODO: one of
	{
	    inclusiveOrExpression ();
	}
	{
	    conditionalAndExpression ();
	    match (Token.LOGICAL_AND);
	    inclusiveOrExpression ();
	}
    }

    private void inclusiveOrExpression () {
	// TODO: one of
	{
	    exclusiveOrExpression ();
	}
	{
	    inclusiveOrExpression ();
	    match (Token.BIT_OR);
	    exclusiveOrExpression ();
	}
    }

    private void exclusiveOrExpression () {
	// TODO: one of
	{
	    andExpression ();
	}
	{
	    exclusiveOrExpression ();
	    match (Token.BIT_XOR);
	    andExpression ();
	}
    }

    private void andExpression () {
	// TODO: one of
	{
	    equalityExpression ();
	}
	{
	    andExpression ();
	    match (Token.BIT_AND);
	    equalityExpression ();
	}
    }

    private void equalityExpression () {
	// TODO: one of
	{
	    relationalExpression ();
	}
	{
	    equalityExpression ();
	    match (Token.DOUBLE_EQUAL);
	    relationalExpression ();
	}
	{
	    equalityExpression ();
	    match (Token.NOT_EQUAL);
	    relationalExpression ();
	}
    }

    private void relationalExpression () {
	// TODO: one of
	{
	    shiftExpression ();
	}
	{
	    relationalExpression ();
	    match (Token.LT);
	    shiftExpression ();
	}
	{
	    relationalExpression ();
	    match (Token.GT);
	    shiftExpression ();
	}
	{
	    relationalExpression ();
	    match (Token.LE);
	    shiftExpression ();
	}
	{
	    relationalExpression ();
	    match (Token.GE);
	    shiftExpression ();
	}
	{
	    relationalExpression ();
	    match (Token.INSTANCEOF);
	    referenceType ();
	}
    }

    private void shiftExpression () {
	// TODO: one of
	{
	    additiveExpression ();
	}
	{
	    shiftExpression ();
	    match (Token.LEFT_SHIFT);
	    additiveExpression ();
	}
	{
	    shiftExpression ();
	    match (Token.RIGHT_SHIFT);
	    additiveExpression ();
	}
	{
	    shiftExpression ();
	    match (Token.RIGHT_SHIFT_UNSIGNED);
	    additiveExpression ();
	}
    }

    private void additiveExpression () {
	// TODO: one of
	{
	    multiplicativeExpression ();
	}
	{
	    additiveExpression ();
	    match (Token.PLUS);
	    multiplicativeExpression ();
	}
	{
	    additiveExpression ();
	    match (Token.MINUS);
	    multiplicativeExpression ();
	}
    }

    private void multiplicativeExpression () {
	// TODO: one of
	{
	    unaryExpression ();
	}
	{
	    multiplicativeExpression ();
	    match (Token.MULTIPLY);
	    unaryExpression ();
	}
	{
	    multiplicativeExpression ();
	    match (Token.DIVIDE);
	    unaryExpression ();
	}
	{
	    multiplicativeExpression ();
	    match (Token.REMAINDER);
	    unaryExpression ();
	}
    }

    private void unaryExpression () {
	// TODO: one of
	{
	    preIncrementExpression ();
	}
	{
	    preDecrementExpression ();
	}
	{
	    match (Token.PLUS);
	    unaryExpression ();
	}
	{
	    match (Token.MINUS);
	    unaryExpression ();
	}
	{
	    unaryExpressionNotPlusMinus ();
	}
    }

    private void preIncrementExpression () {
	match (Token.INCREMENT);
	unaryExpression ();
    }

    private void preDecrementExpression () {
	match (Token.DECREMENT);
	unaryExpression ();
    }

    private void unaryExpressionNotPlusMinus () {
	switch (nextToken ()) {
	case TILDE:
	case NOT:
	    unaryExpression ();
	    break;
	case LEFT_PARENTHESIS: // TODO: may be (expression) as well
	    castExpression();
	    break;
	default:
	    postfixExpression ();
	}
    }

    private void postfixExpression () {
	// TODO: one of
	{
	    primary ();
	}
	{  // identifiersConnectedByDots
	    expressionName ();
	}
	{
	    postIncrementExpression ();
	}
	{
	    postDecrementExpression ();
	}
    }

    private void postIncrementExpression () {
	postfixExpression ();
	match (Token.INCREMENT);
    }

    private void postDecrementExpression () {
	postfixExpression ();
	match (Token.DECREMENT);
    }

    private void castExpression () {
	match (Token.LEFT_PARENTHESIS);
	annotations ();
	if (primitiveTypes.contains (nextToken ())) {
	    unannPrimitiveType ();
	    match (Token.RIGHT_PARENTHESIS);
	    unaryExpression();
	} else {
	    unannReferenceType ();
	    additionalBounds ();
	    match (Token.RIGHT_PARENTHESIS);
	    // TODO: one of
	    {
		unaryExpressionNotPlusMinus ();
	    }
	    {
		lambdaExpression ();
	    }
	}
    }

    /* End of §15 */

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

    private void addParserError (String error) {
	diagnostics.report (new SourceDiagnostics (path,
						   lexer.getTokenStartPos (),
						   lexer.getTokenEndPos (),
						   lexer.getLineNumber (),
						   lexer.getTokenColumn (),
						   error));
    }
}
