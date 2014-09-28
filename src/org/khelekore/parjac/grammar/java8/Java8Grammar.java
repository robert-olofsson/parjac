package org.khelekore.parjac.grammar.java8;

import org.khelekore.parjac.parser.LRParser;
import static org.khelekore.parjac.lexer.Token.*;

public class Java8Grammar {
    private final LRParser lr;

    public Java8Grammar () {
	lr = new LRParser ();
	addRules ();
	lr.build ();
    }

    public LRParser getLRParser () {
	return lr;
    }

    private void addRules () {
	// First rule should be the goal rule
	lr.addRule ("Goal", "CompilationUnit");

	// Productions from §3 (Lexical Structure)
	lr.addRule ("Literal",
		    lr.oneOf ("IntegerLiteral",
			      "FloatingPointLiteral",
			      "BooleanLiteral",
			      CHARACTER_LITERAL,
			      STRING_LITERAL,
			      NULL));
	lr.addRule ("IntegerLiteral", INT_LITERAL);
	lr.addRule ("IntegerLiteral", LONG_LITERAL);
	lr.addRule ("FloatingPointLiteral", FLOAT_LITERAL);
	lr.addRule ("FloatingPointLiteral", DOUBLE_LITERAL);
	lr.addRule ("BooleanLiteral", TRUE);
	lr.addRule ("BooleanLiteral", FALSE);
	// End of §3

	// Productions from §4 (Types, Values, and Variables)
	lr.addRule ("Type", "PrimitiveType");
	lr.addRule ("Type", "ReferenceType");
	lr.addRule ("PrimitiveType",
		    lr.oneOf (lr.sequence (lr.zeroOrMore ("Annotation"), "NumericType"),
			      lr.sequence (lr.zeroOrMore ("Annotation"), BOOLEAN)));
	lr.addRule ("NumericType",
		    lr.oneOf ("IntegralType",
			      "FloatingPointType"));
	lr.addRule ("IntegralType",
		    lr.oneOf (BYTE, SHORT, INT, LONG, CHAR));
	lr.addRule ("FloatingPointType",
		    lr.oneOf (FLOAT, DOUBLE));
	lr.addRule("ReferenceType",
		   lr.oneOf ("ClassOrInterfaceType",
			     "TypeVariable",
			     "ArrayType"));
	lr.addRule ("ClassOrInterfaceType",
		    lr.oneOf ("ClassType",
			      "InterfaceType"));
	lr.addRule ("ClassType",
		    lr.oneOf (lr.sequence (lr.zeroOrMore ("Annotation"),
					   IDENTIFIER,
					   lr.zeroOrOne ("TypeArguments")),
			      lr.sequence ("ClassOrInterfaceType", DOT, lr.zeroOrMore ("Annotation"),
					   IDENTIFIER, lr.zeroOrOne ("TypeArguments"))));
	lr.addRule ("InterfaceType", "ClassType");
	lr.addRule ("TypeVariable", lr.zeroOrMore ("Annotation"), IDENTIFIER);
	lr.addRule ("ArrayType",
		    lr.oneOf (lr.sequence ("PrimitiveType", "Dims"),
			      lr.sequence ("ClassOrInterfaceType", "Dims"),
			      lr.sequence ("TypeVariable", "Dims")));
	lr.addRule ("Dims",
		    lr.zeroOrMore ("Annotation"), LEFT_BRACKET, RIGHT_BRACKET,
		    lr.zeroOrMore (lr.zeroOrMore ("Annotation"),  LEFT_BRACKET, RIGHT_BRACKET));
	lr.addRule ("TypeParameter",
		    lr.zeroOrMore ("TypeParameterModifier"), IDENTIFIER, lr.zeroOrOne ("TypeBound"));
	lr.addRule ("TypeParameterModifier", "Annotation");
	lr.addRule ("TypeBound",
		    lr.oneOf (lr.sequence (EXTENDS, "TypeVariable"),
			      lr.sequence (EXTENDS, "ClassOrInterfaceType", lr.zeroOrMore ("AdditionalBound"))));
	lr.addRule ("AdditionalBound", AND, "InterfaceType");
	lr.addRule ("TypeArguments", LT, "TypeArgumentList", GT);
	lr.addRule ("TypeArgumentList",
		    "TypeArgument", lr.zeroOrMore (COMMA, "TypeArgument"));
	lr.addRule ("TypeArgument",
		    lr.oneOf ("ReferenceType",
			      "Wildcard"));
	lr.addRule ("Wildcard",
		    lr.zeroOrMore ("Annotation"), QUESTIONMARK, lr.zeroOrOne ("WildcardBounds"));
	lr.addRule ("WildcardBounds",
		    lr.oneOf (lr.sequence (EXTENDS, "ReferenceType"),
			      lr.sequence (SUPER, "ReferenceType")));
	// End of §4

	// Productions from §6 Names
	lr.addRule ("TypeName",
		    lr.oneOf (IDENTIFIER,
			      lr.sequence ("PackageOrTypeName", DOT, IDENTIFIER)));
	lr.addRule ("PackageOrTypeName",
		    lr.oneOf (IDENTIFIER,
			      lr.sequence ("PackageOrTypeName", DOT, IDENTIFIER)));
	lr.addRule ("ExpressionName",
		    lr.oneOf (IDENTIFIER,
			      lr.sequence ("AmbiguousName", DOT, IDENTIFIER)));
	lr.addRule ("MethodName", IDENTIFIER);
	lr.addRule ("AmbiguousName",
		    lr.oneOf (IDENTIFIER,
			      lr.sequence ("AmbiguousName", DOT, IDENTIFIER)));
	// End of §6

	// Productions from §7 (Packages)
	lr.addRule ("CompilationUnit",
		    lr.zeroOrOne ("PackageDeclaration"),
		    lr.zeroOrMore ("ImportDeclaration"),
		    lr.zeroOrMore ("TypeDeclaration"));
	lr.addRule ("PackageDeclaration",
		    lr.zeroOrMore ("PackageModifier"), PACKAGE, IDENTIFIER,
		    lr.zeroOrMore (DOT, IDENTIFIER), SEMICOLON);
	lr.addRule ("PackageModifier", "Annotation");
	lr.addRule ("ImportDeclaration",
		    lr.oneOf ("SingleTypeImportDeclaration",
			      "TypeImportOnDemandDeclaration",
			      "SingleStaticImportDeclaration",
			      "StaticImportOnDemandDeclaration"));
	lr.addRule ("SingleTypeImportDeclaration",
		    IMPORT, "TypeName", SEMICOLON);
	lr.addRule ("TypeImportOnDemandDeclaration",
		    IMPORT, "PackageOrTypeName", DOT, MULTIPLY, SEMICOLON);
	lr.addRule ("SingleStaticImportDeclaration",
		    IMPORT, STATIC, "TypeName", DOT, IDENTIFIER, SEMICOLON);
	lr.addRule ("StaticImportOnDemandDeclaration",
		    IMPORT, STATIC, "TypeName", DOT, MULTIPLY, SEMICOLON);
	lr.addRule ("TypeDeclaration",
		    lr.oneOf ("ClassDeclaration",
			      "InterfaceDeclaration",
			      SEMICOLON));
	// End of §7

	// Productions from §8 (Classes)
	lr.addRule ("ClassDeclaration",
		    lr.oneOf ("NormalClassDeclaration", "EnumDeclaration"));
	lr.addRule ("NormalClassDeclaration",
		    lr.zeroOrMore ("ClassModifier"), CLASS, IDENTIFIER,
		    lr.zeroOrOne ("TypeParameters"), lr.zeroOrOne ("Superclass"),
		    lr.zeroOrOne ("Superinterfaces"), "ClassBody");
	lr.addRule ("ClassModifier",
		    lr.oneOf ("Annotation", PUBLIC, PROTECTED, PRIVATE,
			      ABSTRACT, STATIC, FINAL, STRICTFP));
	lr.addRule ("TypeParameters",
		    LT, "TypeParameterList", GT);
	lr.addRule ("TypeParameterList",
		    "TypeParameter", lr.zeroOrMore (COMMA, "TypeParameter"));
	lr.addRule ("Superclass", EXTENDS, "ClassType");
	lr.addRule ("Superinterfaces", IMPLEMENTS, "InterfaceTypeList");
	lr.addRule ("InterfaceTypeList", "InterfaceType", lr.zeroOrMore (COMMA, "InterfaceType"));
	lr.addRule ("ClassBody",
		    LEFT_CURLY,
		    lr.zeroOrMore ("ClassBodyDeclaration"),
		    RIGHT_CURLY);
	lr.addRule ("ClassBodyDeclaration",
		    lr.oneOf ("ClassMemberDeclaration",
			      "InstanceInitializer",
			      "StaticInitializer",
			      "ConstructorDeclaration"));
	lr.addRule ("ClassMemberDeclaration",
		    lr.oneOf ("FieldDeclaration",
			      "MethodDeclaration",
			      "ClassDeclaration",
			      "InterfaceDeclaration",
			      SEMICOLON));
	lr.addRule ("FieldDeclaration",
		    lr.zeroOrMore ("FieldModifier"), "UnannType", "VariableDeclaratorList", SEMICOLON);
	lr.addRule ("FieldModifier",
		    lr.oneOf ("Annotation",
			      PUBLIC,
			      PROTECTED,
			      PRIVATE,
			      STATIC,
			      FINAL,
			      TRANSIENT,
			      VOLATILE));
	lr.addRule ("VariableDeclaratorList",
		    "VariableDeclarator", lr.zeroOrMore (COMMA, "VariableDeclarator"));
	lr.addRule ("VariableDeclarator",
		    "VariableDeclaratorId", lr.zeroOrOne (EQUAL, "VariableInitializer")
	    );
	lr.addRule ("VariableDeclaratorId",
		    IDENTIFIER, lr.zeroOrOne ("Dims"));
	lr.addRule ("VariableInitializer",
		    lr.oneOf ("Expression",
			      "ArrayInitializer"));
	lr.addRule ("UnannType",
		    lr.oneOf ("UnannPrimitiveType",
			      "UnannReferenceType"));
	lr.addRule ("UnannPrimitiveType",
		    lr.oneOf ("NumericType",
			      BOOLEAN));
	lr.addRule ("UnannReferenceType",
		    lr.oneOf ("UnannClassOrInterfaceType",
			      "UnannTypeVariable",
			      "UnannArrayType"));
	lr.addRule ("UnannClassOrInterfaceType",
		    lr.oneOf ("UnannClassType",
			      "UnannInterfaceType"));
	lr.addRule ("UnannClassType",
		    lr.oneOf (lr.sequence (IDENTIFIER, lr.zeroOrOne ("TypeArguments")),
			      lr.sequence ("UnannClassOrInterfaceType", DOT, lr.zeroOrMore ("Annotation"),
					   IDENTIFIER, lr.zeroOrOne ("TypeArguments"))));
	lr.addRule ("UnannInterfaceType", "UnannClassType");
	lr.addRule ("UnannTypeVariable", IDENTIFIER);
	lr.addRule ("UnannArrayType",
		    lr.oneOf (lr.sequence ("UnannPrimitiveType", "Dims"),
			      lr.sequence ("UnannClassOrInterfaceType", "Dims"),
			      lr.sequence ("UnannTypeVariable", "Dims")));
	lr.addRule ("MethodDeclaration",
		    lr.zeroOrMore ("MethodModifier"), "MethodHeader", "MethodBody");
	lr.addRule ("MethodModifier",
		    lr.oneOf ("Annotation",
			      PUBLIC,
			      PROTECTED,
			      PRIVATE,
			      ABSTRACT,
			      STATIC,
			      FINAL,
			      SYNCHRONIZED,
			      NATIVE,
			      STRICTFP));
	lr.addRule ("MethodHeader",
		    lr.oneOf (lr.sequence ("Result", "MethodDeclarator", lr.zeroOrOne ("Throws")),
			      lr.sequence ("TypeParameters", lr.zeroOrMore ("Annotation"),
					   "Result", "MethodDeclarator", lr.zeroOrOne ("Throws"))));
	lr.addRule ("Result",
		    lr.oneOf ("UnannType",
			      VOID));
	lr.addRule ("MethodDeclarator",
		    IDENTIFIER, LEFT_PARENTHESIS, lr.zeroOrOne ("FormalParameterList"), RIGHT_PARENTHESIS,
		    lr.zeroOrOne ("Dims"));
	lr.addRule ("FormalParameterList",
		    lr.oneOf (lr.sequence ("FormalParameters", COMMA, "LastFormalParameter"),
			      "LastFormalParameter"));
	lr.addRule ("FormalParameters",
		    lr.oneOf (lr.sequence ("FormalParameter", lr.zeroOrMore (COMMA, "FormalParameter")),
			      lr.sequence ("ReceiverParameter", lr.zeroOrMore (COMMA, "FormalParameter"))));
	lr.addRule ("FormalParameter",
		    lr.zeroOrMore ("VariableModifier"), "UnannType", "VariableDeclaratorId");
	lr.addRule ("VariableModifier",
		    lr.oneOf ("Annotation",
			      FINAL));
	lr.addRule ("LastFormalParameter",
		    lr.oneOf (lr.sequence (lr.zeroOrMore ("VariableModifier"), "UnannType",
					   lr.zeroOrMore ("Annotation"),
					   ELLIPSIS, "VariableDeclaratorId"),
			      "FormalParameter"));
	lr.addRule ("ReceiverParameter",
		    lr.zeroOrMore ("Annotation"), "UnannType",
		    lr.zeroOrOne (IDENTIFIER, DOT), THIS);
	lr.addRule ("Throws",
		    THROWS, "ExceptionTypeList");
	lr.addRule ("ExceptionTypeList",
		    "ExceptionType", lr.zeroOrMore (COMMA, "ExceptionType"));
	lr.addRule ("ExceptionType",
		    lr.oneOf ("ClassType",
			      "TypeVariable"));
	lr.addRule ("MethodBody",
		    lr.oneOf ("Block",
			      SEMICOLON));
	lr.addRule ("InstanceInitializer", "Block");
	lr.addRule ("StaticInitializer", STATIC, "Block");
	lr.addRule ("ConstructorDeclaration",
		    lr.zeroOrMore ("ConstructorModifier"), "ConstructorDeclarator",
		    lr.zeroOrOne ("Throws"), "ConstructorBody");
	lr.addRule ("ConstructorModifier",
		    lr.oneOf ("Annotation",
			      PUBLIC,
			      PROTECTED,
			      PRIVATE));
	lr.addRule ("ConstructorDeclarator",
		    lr.zeroOrOne ("TypeParameters"), "SimpleTypeName",
		    LEFT_PARENTHESIS, lr.zeroOrOne ("FormalParameterList"), RIGHT_PARENTHESIS);
	lr.addRule ("SimpleTypeName", IDENTIFIER);
	lr.addRule ("ConstructorBody",
		    LEFT_CURLY,
		    lr.zeroOrOne ("ExplicitConstructorInvocation"), lr.zeroOrOne ("BlockStatements"),
		    RIGHT_CURLY);
	lr.addRule ("ExplicitConstructorInvocation",
		    lr.oneOf (lr.sequence (lr.zeroOrOne ("TypeArguments"), THIS,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   SEMICOLON),
			      lr.sequence (lr.zeroOrOne ("TypeArguments"), SUPER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   SEMICOLON),
			      lr.sequence ("ExpressionName", DOT,
					   lr.zeroOrOne ("TypeArguments"), SUPER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   SEMICOLON),
			      lr.sequence ("Primary", DOT,
					   lr.zeroOrOne ("TypeArguments"), SUPER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   SEMICOLON)));
	lr.addRule ("EnumDeclaration",
		    lr.zeroOrMore ("ClassModifier"),
		    ENUM, IDENTIFIER, lr.zeroOrOne ("Superinterfaces"), "EnumBody");
	lr.addRule ("EnumBody",
		    LEFT_CURLY,
		    lr.zeroOrOne ("EnumConstantList"), lr.zeroOrOne (COMMA),
		    lr.zeroOrOne ("EnumBodyDeclarations"),
		    RIGHT_CURLY);
	lr.addRule ("EnumConstantList",
		    "EnumConstant", lr.zeroOrMore (COMMA, "EnumConstant"));
	lr.addRule ("EnumConstant",
		    lr.zeroOrMore ("EnumConstantModifier"), IDENTIFIER,
		    lr.zeroOrOne (LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
		    lr.zeroOrOne ("ClassBody"));
	lr.addRule ("EnumConstantModifier",
		    "Annotation");
	lr.addRule ("EnumBodyDeclarations",
		    SEMICOLON, lr.zeroOrMore ("ClassBodyDeclaration"));
	// End of §8

	// Productions from §9 (Interfaces)
	lr.addRule ("InterfaceDeclaration",
		    lr.oneOf ("NormalInterfaceDeclaration", "AnnotationTypeDeclaration"));
	lr.addRule ("NormalInterfaceDeclaration",
		    lr.zeroOrMore ("InterfaceModifier"), INTERFACE, IDENTIFIER,
		    lr.zeroOrOne ("TypeParameters"), lr.zeroOrOne ("ExtendsInterfaces"), "InterfaceBody");
	lr.addRule ("InterfaceModifier",
		    lr.oneOf ("Annotation", PUBLIC, PROTECTED, PRIVATE,
			      ABSTRACT, STATIC, STRICTFP));
	lr.addRule ("ExtendsInterfaces",
		    EXTENDS, "InterfaceTypeList");
	lr.addRule ("InterfaceBody",
		    LEFT_CURLY,
		    lr.zeroOrMore ("InterfaceMemberDeclaration"),
		    RIGHT_CURLY);
	lr.addRule ("InterfaceMemberDeclaration",
		    lr.oneOf ("ConstantDeclaration",
			      "InterfaceMethodDeclaration",
			      "ClassDeclaration",
			      "InterfaceDeclaration",
			      SEMICOLON));
	lr.addRule ("ConstantDeclaration",
		    lr.zeroOrMore ("ConstantModifier"), "UnannType", "VariableDeclaratorList", SEMICOLON);
	lr.addRule ("ConstantModifier",
		    lr.oneOf ("Annotation",
			      PUBLIC,
			      STATIC,
			      FINAL));
	lr.addRule ("InterfaceMethodDeclaration",
		    lr.zeroOrMore ("InterfaceMethodModifier"), "MethodHeader", "MethodBody");
	lr.addRule ("InterfaceMethodModifier",
		    lr.oneOf ("Annotation",
			      PUBLIC,
			      ABSTRACT,
			      DEFAULT,
			      STATIC,
			      STRICTFP));
	lr.addRule ("AnnotationTypeDeclaration",
		    lr.zeroOrMore ("InterfaceModifier"),
		    AT, INTERFACE, IDENTIFIER, "AnnotationTypeBody");
	lr.addRule ("AnnotationTypeBody",
		    LEFT_CURLY,
		    lr.zeroOrMore ("AnnotationTypeMemberDeclaration"),
		    RIGHT_CURLY);

	lr.addRule ("AnnotationTypeMemberDeclaration",
		    lr.oneOf ("AnnotationTypeElementDeclaration",
			      "ConstantDeclaration",
			      "ClassDeclaration",
			      "InterfaceDeclaration",
			      SEMICOLON));
	lr.addRule ("AnnotationTypeElementDeclaration",
		    lr.zeroOrMore ("AnnotationTypeElementModifier"), "UnannType",
		    IDENTIFIER, LEFT_PARENTHESIS, RIGHT_PARENTHESIS, lr.zeroOrOne ("Dims"),
		    lr.zeroOrOne ("DefaultValue"), SEMICOLON);
	lr.addRule ("AnnotationTypeElementModifier",
		    lr.oneOf ("Annotation",
			      PUBLIC,
			      ABSTRACT));
	lr.addRule ("DefaultValue",
		    DEFAULT, "ElementValue");
	lr.addRule ("Annotation",
		    lr.oneOf ("NormalAnnotation", "MarkerAnnotation", "SingleElementAnnotation"));
	lr.addRule ("NormalAnnotation",
		    AT, "TypeName", LEFT_PARENTHESIS, lr.zeroOrOne ("ElementValuePairList"));
	lr.addRule ("ElementValuePairList",
		    "ElementValuePair", lr.zeroOrMore (COMMA, "ElementValuePair"));
	lr.addRule ("ElementValuePair",
		    IDENTIFIER, EQUAL, "ElementValue");
	lr.addRule ("ElementValue",
		    lr.oneOf (
			"ConditionalExpression",
			"ElementValueArrayInitializer",
			"Annotation"));
	lr.addRule ("ElementValueArrayInitializer",
		    LEFT_CURLY, lr.zeroOrOne ("ElementValueList"), lr.zeroOrOne (COMMA));
	lr.addRule ("ElementValueList",
		    "ElementValue", lr.zeroOrMore (COMMA, "ElementValue"));
	lr.addRule ("MarkerAnnotation",
		    AT, "TypeName");
	lr.addRule ("SingleElementAnnotation",
		    AT, "TypeName", LEFT_PARENTHESIS, "ElementValue", RIGHT_PARENTHESIS);

	// End of §9

	// Productions from §10 (Arrays)
	lr.addRule ("ArrayInitializer",
		    LEFT_CURLY, lr.zeroOrOne ("VariableInitializerList"),
		    lr.zeroOrOne (COMMA), RIGHT_CURLY);
	lr.addRule ("VariableInitializerList",
		    "VariableInitializer", lr.zeroOrMore (COMMA, "VariableInitializer"));

	// End of §10

	// Productions from §14 (Blocks and Statements)
	lr.addRule ("Block",
		    LEFT_CURLY, lr.zeroOrOne ("BlockStatements"), RIGHT_CURLY);
	lr.addRule ("BlockStatements",
		    "BlockStatement", lr.zeroOrMore ("BlockStatement"));
	lr.addRule ("BlockStatement",
		    lr.oneOf ("LocalVariableDeclarationStatement",
			      "ClassDeclaration",
			      "Statement"));
	lr.addRule ("LocalVariableDeclarationStatement", "LocalVariableDeclaration", SEMICOLON);
	lr.addRule ("LocalVariableDeclaration",
		    lr.zeroOrMore ("VariableModifier"), "UnannType", "VariableDeclaratorList");
	lr.addRule ("Statement",
		    lr.oneOf ("StatementWithoutTrailingSubstatement",
			      "LabeledStatement",
			      "IfThenStatement",
			      "IfThenElseStatement",
			      "WhileStatement",
			      "ForStatement"));
	lr.addRule ("StatementNoShortIf",
		    lr.oneOf ("StatementWithoutTrailingSubstatement",
			      "LabeledStatementNoShortIf",
			      "IfThenElseStatementNoShortIf",
			      "WhileStatementNoShortIf",
			      "ForStatementNoShortIf"));
	lr.addRule ("StatementWithoutTrailingSubstatement",
		    lr.oneOf ("Block",
			      "EmptyStatement",
			      "ExpressionStatement",
			      "AssertStatement",
			      "SwitchStatement",
			      "DoStatement",
			      "BreakStatement",
			      "ContinueStatement",
			      "ReturnStatement",
			      "SynchronizedStatement",
			      "ThrowStatement",
			      "TryStatement"));
	lr.addRule ("EmptyStatement", SEMICOLON);
	lr.addRule ("LabeledStatement", IDENTIFIER, COLON, "Statement");
	lr.addRule ("LabeledStatementNoShortIf", IDENTIFIER, COLON, "StatementNoShortIf");
	lr.addRule ("ExpressionStatement", "StatementExpression", SEMICOLON);
	lr.addRule ("StatementExpression",
		    lr.oneOf ("Assignment",
			      "PreIncrementExpression",
			      "PreDecrementExpression",
			      "PostIncrementExpression",
			      "PostDecrementExpression",
			      "MethodInvocation",
			      "ClassInstanceCreationExpression"));
	lr.addRule ("IfThenStatement",
		    IF, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "Statement");
	lr.addRule ("IfThenElseStatement",
		    IF, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS,
		    "StatementNoShortIf", ELSE, "Statement");
	lr.addRule ("IfThenElseStatementNoShortIf",
		    IF, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS,
		    "StatementNoShortIf", ELSE, "StatementNoShortIf");
	lr.addRule ("AssertStatement",
		    lr.oneOf (lr.sequence (ASSERT, "Expression", SEMICOLON),
			      lr.sequence (ASSERT, "Expression", COLON, "Expression", SEMICOLON)));
	lr.addRule ("SwitchStatement",
		    SWITCH, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "SwitchBlock");
	lr.addRule ("SwitchBlock",
		    LEFT_CURLY, lr.zeroOrMore ("SwitchBlockStatementGroup"),
		    lr.zeroOrMore ("SwitchLabel"), RIGHT_CURLY);
	lr.addRule ("SwitchBlockStatementGroup",
		    "SwitchLabels", "BlockStatements");
	lr.addRule ("SwitchLabels",
		    "SwitchLabel", lr.zeroOrMore ("SwitchLabel"));
	lr.addRule ("SwitchLabel",
		    lr.oneOf (lr.sequence (CASE, "ConstantExpression", COLON),
			      lr.sequence (CASE, "EnumConstantName", COLON),
			      lr.sequence (DEFAULT, COLON)));
	lr.addRule ("EnumConstantName", IDENTIFIER);
	lr.addRule ("WhileStatement",
		    WHILE, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "Statement");
	lr.addRule ("WhileStatementNoShortIf",
		    WHILE, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "StatementNoShortIf");
	lr.addRule ("DoStatement",
		    DO, "Statement", WHILE, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, SEMICOLON);
	lr.addRule ("ForStatement",
		    lr.oneOf ("BasicForStatement",
			      "EnhancedForStatement"));
	lr.addRule ("ForStatementNoShortIf",
		    lr.oneOf ("BasicForStatementNoShortIf",
			      "EnhancedForStatementNoShortIf"));
	lr.addRule ("BasicForStatement",
		    FOR, LEFT_PARENTHESIS,
		    lr.zeroOrOne ("ForInit"), SEMICOLON,
		    lr.zeroOrOne ("Expression"), SEMICOLON,
		    lr.zeroOrOne ("ForUpdate"),
		    RIGHT_PARENTHESIS, "Statement");
	lr.addRule ("BasicForStatementNoShortIf",
		    FOR, LEFT_PARENTHESIS,
		    lr.zeroOrOne ("ForInit"), SEMICOLON,
		    lr.zeroOrOne ("Expression"), SEMICOLON,
		    lr.zeroOrOne ("ForUpdate"),
		    RIGHT_PARENTHESIS, "StatementNoShortIf");
	lr.addRule ("ForInit",
		    lr.oneOf ("StatementExpressionList",
			      "LocalVariableDeclaration"));
	lr.addRule ("ForUpdate", "StatementExpressionList");
	lr.addRule ("StatementExpressionList",
		    "StatementExpression", lr.zeroOrMore (COMMA, "StatementExpression"));
	lr.addRule ("EnhancedForStatement",
		    FOR, LEFT_PARENTHESIS, lr.zeroOrMore ("VariableModifier"),
		    "UnannType", "VariableDeclaratorId", COLON, "Expression", RIGHT_PARENTHESIS,
		    "Statement");
	lr.addRule ("EnhancedForStatementNoShortIf",
		    FOR, LEFT_PARENTHESIS, lr.zeroOrMore ("VariableModifier"),
		    "UnannType", "VariableDeclaratorId", COLON, "Expression", RIGHT_PARENTHESIS,
		    "StatementNoShortIf");
	lr.addRule ("BreakStatement",
		    BREAK, lr.zeroOrOne (IDENTIFIER), SEMICOLON);
	lr.addRule ("ContinueStatement",
		    CONTINUE, lr.zeroOrOne (IDENTIFIER), SEMICOLON);
	lr.addRule ("ReturnStatement",
		    RETURN, lr.zeroOrOne ("Expression"), SEMICOLON);
	lr.addRule ("ThrowStatement",
		    THROW, "Expression", SEMICOLON);
	lr.addRule ("SynchronizedStatement",
		    SYNCHRONIZED, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "Block");
	lr.addRule ("TryStatement",
		    lr.oneOf (lr.sequence (TRY, "Block", "Catches"),
			      lr.sequence (TRY, "Block", lr.zeroOrOne ("Catches"), "Finally"),
			      "TryWithResourcesStatement"));
	lr.addRule ("Catches",
		    "CatchClause", lr.zeroOrMore ("CatchClause"));
	lr.addRule ("CatchClause",
		    CATCH, LEFT_PARENTHESIS, "CatchFormalParameter", RIGHT_PARENTHESIS, "Block");
	lr.addRule ("CatchFormalParameter",
		    lr.zeroOrMore ("VariableModifier"), "CatchType", "VariableDeclaratorId");
	lr.addRule ("CatchType",
		    "UnannClassType", lr.zeroOrMore (OR, "ClassType"));
	lr.addRule ("Finally",
		    FINALLY, "Block");
	lr.addRule ("TryWithResourcesStatement",
		    TRY, "ResourceSpecification", "Block", lr.zeroOrOne ("Catches"), lr.zeroOrOne ("Finally"));
	lr.addRule ("ResourceSpecification",
		    LEFT_PARENTHESIS, "ResourceList", lr.zeroOrOne (SEMICOLON), RIGHT_PARENTHESIS);
	lr.addRule ("ResourceList",
		    "Resource", lr.zeroOrMore (SEMICOLON, "Resource"));
	lr.addRule ("Resource",
		    lr.zeroOrMore ("VariableModifier"), "UnannType", "VariableDeclaratorId", EQUAL, "Expression");
	// End of §14

	// Productions from §15 (Expressions)
	lr.addRule ("Primary",
		    lr.oneOf ("PrimaryNoNewArray",
			      "ArrayCreationExpression"));
	lr.addRule ("PrimaryNoNewArray",
		    lr.oneOf ("Literal",
			      lr.sequence ("TypeName", lr.zeroOrMore (LEFT_BRACKET, RIGHT_BRACKET), DOT, CLASS),
			      lr.sequence (VOID, DOT, CLASS),
			      THIS,
			      lr.sequence ("TypeName", DOT, THIS),
			      lr.sequence (LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS),
			      "ClassInstanceCreationExpression",
			      "FieldAccess",
			      "ArrayAccess",
			      "MethodInvocation",
			      "MethodReference"));
	lr.addRule ("ClassInstanceCreationExpression",
		    lr.oneOf (lr.sequence (NEW, lr.zeroOrOne ("TypeArguments"),
					   lr.zeroOrMore ("Annotation"), IDENTIFIER,
					   lr.zeroOrOne ("TypeArgumentsOrDiamond"),
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   lr.zeroOrOne ("ClassBody")),
			      lr.sequence ("ExpressionName", DOT,
					   NEW, lr.zeroOrOne ("TypeArguments"),
					   lr.zeroOrMore ("Annotation"), IDENTIFIER,
					   lr.zeroOrOne ("TypeArgumentsOrDiamond"),
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   lr.zeroOrOne ("ClassBody")),
			      lr.sequence ("Primary", DOT, NEW, lr.zeroOrOne ("TypeArguments"),
					   lr.zeroOrMore ("Annotation"), IDENTIFIER,
					   lr.zeroOrOne ("TypeArgumentsOrDiamond"),
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   lr.zeroOrOne ("ClassBody"))));
	lr.addRule ("TypeArgumentsOrDiamond",
		    lr.oneOf ("TypeArguments",
			      lr.sequence (LT, GT)));
	lr.addRule ("FieldAccess",
		    lr.oneOf (lr.sequence ("Primary", DOT, IDENTIFIER),
			      lr.sequence (SUPER, DOT, IDENTIFIER),
			      lr.sequence ("TypeName", DOT, SUPER, DOT, IDENTIFIER)));
	lr.addRule ("ArrayAccess",
		    lr.oneOf (lr.sequence ("ExpressionName", LEFT_BRACKET, "Expression", RIGHT_BRACKET),
			      lr.sequence ("PrimaryNoNewArray", LEFT_BRACKET, "Expression", RIGHT_BRACKET)));
	lr.addRule ("MethodInvocation",
		    lr.oneOf (lr.sequence ("MethodName",
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence ("TypeName", DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence ("ExpressionName", DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence ("Primary", DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence (SUPER, DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence ("TypeName", DOT, SUPER, DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS)));
	lr.addRule ("ArgumentList",
		    "Expression", lr.zeroOrMore (COMMA, "Expression"));
	lr.addRule ("MethodReference",
		    lr.oneOf (lr.sequence ("ExpressionName", DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence ("ReferenceType", DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence ("Primary", DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence (SUPER, DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence ("TypeName", DOT, SUPER, DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence ("ClassType", DOUBLE_COLON, 
					   lr.zeroOrOne ("TypeArguments"), NEW),
			      lr.sequence ("ArrayType", DOUBLE_COLON, NEW)));
	lr.addRule ("ArrayCreationExpression",
		    lr.oneOf (lr.sequence (NEW, "PrimitiveType", "DimExprs", lr.zeroOrOne ("Dims")),
			      lr.sequence (NEW, "ClassOrInterfaceType", "DimExprs", lr.zeroOrOne ("Dims")),
			      lr.sequence (NEW, "PrimitiveType", "Dims", "ArrayInitializer"),
			      lr.sequence (NEW, "ClassOrInterfaceType", "Dims", "ArrayInitializer")));
	lr.addRule ("DimExprs",
		    "DimExpr", lr.zeroOrMore ("DimExpr"));
	lr.addRule ("DimExpr",
		    lr.zeroOrMore ("Annotation"), LEFT_BRACKET, "Expression", RIGHT_BRACKET);
	lr.addRule ("ConstantExpression", "Expression");
	lr.addRule ("Expression",
		    lr.oneOf ("LambdaExpression", "AssignmentExpression"));
	lr.addRule ("LambdaExpression",
		    "LambdaParameters", ARROW, "LambdaBody");
	lr.addRule ("LambdaParameters",
		    lr.oneOf (IDENTIFIER,
			      lr.sequence (LEFT_PARENTHESIS,
					   lr.zeroOrOne ("FormalParameterList"),
					   RIGHT_PARENTHESIS),
			      lr.sequence (LEFT_PARENTHESIS,
					   "InferredFormalParameterList",
					   RIGHT_PARENTHESIS)));
	lr.addRule ("InferredFormalParameterList",
		    IDENTIFIER, lr.zeroOrMore (COMMA, IDENTIFIER));
	lr.addRule ("LambdaBody",
		    lr.oneOf ("Expression",
			      "Block"));
	lr.addRule ("AssignmentExpression",
		    lr.oneOf ("ConditionalExpression",
			      "Assignment"));
	lr.addRule ("Assignment",
		    "LeftHandSide", "AssignmentOperator", "Expression");
	lr.addRule ("LeftHandSide",
		    lr.oneOf ("ExpressionName",
			      "FieldAccess",
			      "ArrayAccess"));

	lr.addRule ("AssignmentOperator",
		    lr.oneOf (EQUAL,
			      MULTIPLY_EQUAL,
			      DIVIDE_EQUAL,
			      REMAINDER_EQUAL,
			      PLUS_EQUAL,
			      MINUS_EQUAL,
			      LEFT_SHIFT_EQUAL,
			      RIGHT_SHIFT_EQUAL,
			      RIGHT_SHIFT_UNSIGNED_EQUAL,
			      BIT_AND_EQUAL,
			      BIT_XOR_EQUAL,
			      BIT_OR_EQUAL));
	lr.addRule ("ConditionalExpression",
		    lr.oneOf ("ConditionalOrExpression",
			      lr.sequence ("ConditionalOrExpression", QUESTIONMARK,
					   "Expression", COLON, "ConditionalExpression")));
	lr.addRule ("ConditionalOrExpression",
		    lr.oneOf ("ConditionalAndExpression",
			      lr.sequence ("ConditionalOrExpression", LOGICAL_OR, "ConditionalAndExpression")));
	lr.addRule ("ConditionalAndExpression",
		    lr.oneOf ("InclusiveOrExpression",
			      lr.sequence ("ConditionalAndExpression", LOGICAL_AND, "InclusiveOrExpression")));
	lr.addRule ("InclusiveOrExpression",
		    lr.oneOf ("ExclusiveOrExpression",
			      lr.sequence ("InclusiveOrExpression", OR, "ExclusiveOrExpression")));
	lr.addRule ("ExclusiveOrExpression",
		    lr.oneOf ("AndExpression",
			      lr.sequence ("ExclusiveOrExpression", XOR, "AndExpression")));
	lr.addRule ("AndExpression",
		    lr.oneOf ("EqualityExpression",
			      lr.sequence ("AndExpression", AND, "EqualityExpression")));
	lr.addRule ("EqualityExpression",
		    lr.oneOf ("RelationalExpression",
			      lr.sequence ("EqualityExpression", DOUBLE_EQUAL, "RelationalExpression"),
			      lr.sequence ("EqualityExpression", NOT_EQUAL, "RelationalExpression")));
	lr.addRule ("RelationalExpression",
		    lr.oneOf ("ShiftExpression",
			      lr.sequence ("RelationalExpression", LT, "ShiftExpression"),
			      lr.sequence ("RelationalExpression", GT, "ShiftExpression"),
			      lr.sequence ("RelationalExpression", LE, "ShiftExpression"),
			      lr.sequence ("RelationalExpression", GE, "ShiftExpression"),
			      lr.sequence ("RelationalExpression", INSTANCEOF, "ReferenceType")));
	lr.addRule ("ShiftExpression",
		    lr.oneOf ("AdditiveExpression",
			      lr.sequence ("ShiftExpression", LEFT_SHIFT, "AdditiveExpression"),
			      lr.sequence ("ShiftExpression", RIGHT_SHIFT, "AdditiveExpression"),
			      lr.sequence ("ShiftExpression", RIGHT_SHIFT_UNSIGNED, "AdditiveExpression")));
	lr.addRule ("AdditiveExpression",
		    lr.oneOf ("MultiplicativeExpression",
			      lr.sequence ("AdditiveExpression", PLUS, "MultiplicativeExpression"),
			      lr.sequence ("AdditiveExpression", MINUS, "MultiplicativeExpression")));
	lr.addRule ("MultiplicativeExpression",
		    lr.oneOf ("UnaryExpression",
			      lr.sequence ("MultiplicativeExpression", MULTIPLY, "UnaryExpression"),
			      lr.sequence ("MultiplicativeExpression", DIVIDE, "UnaryExpression"),
			      lr.sequence ("MultiplicativeExpression", REMAINDER, "UnaryExpression")));
	lr.addRule ("UnaryExpression",
		    lr.oneOf ("PreIncrementExpression",
			      "PreDecrementExpression",
			      lr.sequence (PLUS, "UnaryExpression"),
			      lr.sequence (MINUS, "UnaryExpression"),
			      "UnaryExpressionNotPlusMinus"));
	lr.addRule ("PreIncrementExpression",
		    INCREMENT, "UnaryExpression");
	lr.addRule ("PreDecrementExpression",
		    DECREMENT, "UnaryExpression");
	lr.addRule ("UnaryExpressionNotPlusMinus",
		    lr.oneOf ("PostfixExpression",
			      lr.sequence (TILDE, "UnaryExpression"),
			      lr.sequence (NOT, "UnaryExpression"),
			      "CastExpression"));
	lr.addRule ("PostfixExpression",
		    lr.oneOf ("Primary",
			      "ExpressionName",
			      "PostIncrementExpression",
			      "PostDecrementExpression"));
	lr.addRule ("PostIncrementExpression",
		    "PostfixExpression", INCREMENT);
	lr.addRule ("PostDecrementExpression",
		    "PostfixExpression", DECREMENT);
	lr.addRule ("CastExpression",
		    lr.oneOf (lr.sequence (LEFT_PARENTHESIS, "PrimitiveType", RIGHT_PARENTHESIS,
					   "UnaryExpression"),
			      lr.sequence (LEFT_PARENTHESIS,
					   "ReferenceType", lr.zeroOrMore ("AdditionalBound"),
					   RIGHT_PARENTHESIS, "UnaryExpressionNotPlusMinus"),
			      lr.sequence (LEFT_PARENTHESIS,
					   "ReferenceType", lr.zeroOrMore ("AdditionalBound"),
					   RIGHT_PARENTHESIS, "LambdaExpression")));
	// End of §15
    }
}