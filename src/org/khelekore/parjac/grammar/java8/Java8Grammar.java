package org.khelekore.parjac.grammar.java8;

import org.khelekore.parjac.parser.LRParser;

import static org.khelekore.parjac.lexer.Token.*;

public class Java8Grammar {
    private final LRParser lr;

    // All modifiers are accepted in the grammar to avoid conflicts, need to check after
    /*
    private Set<Object> CLASS_MODIFIERS =
	new HashSet<> (Arrays.asList ("Annotation", PUBLIC, PROTECTED, PRIVATE,
				      ABSTRACT, STATIC, FINAL, STRICTFP));

    private Set<Object> INTERFACE_MODIFIERS =
	new HashSet<> (Arrays.asList ("Annotation", PUBLIC, PROTECTED, PRIVATE,
				      ABSTRACT, STATIC, STRICTFP));

    private Set<Object> CONSTANT_MODIFIER =
	new HashSet<> (Arrays.asList ("Annotation", PUBLIC, STATIC, FINAL));

    private Set<Object> INTERFACE_METHOD_MODIFIER =
	new HashSet<> (Arrays.asList ("Annotation", PUBLIC, ABSTRACT, DEFAULT, STATIC, STRICTFP));

    private Set<Object> FIELD_MODIFIER =
	new HashSet<> (Arrays.asList ("Annotation", PUBLIC, PROTECTED, PRIVATE,
				      STATIC, FINAL, TRANSIENT, VOLATILE));

    private Set<Object> METHOD_MODIFIER =
	new HashSet<> (Arrays.asList ("Annotation", PUBLIC, PROTECTED, PRIVATE,
				      ABSTRACT, STATIC, FINAL, SYNCHRONIZED, NATIVE, STRICTFP));

    private Set<Object> CONSTRUCTOR_MODIFIER =
	new HashSet<> (Arrays.asList ("Annotation", PUBLIC, PROTECTED, PRIVATE));

    private Set<Object> ANNOTATION_TYPE_ELEMENT_MODIFIER =
	new HashSet<> (Arrays.asList ("Annotation", PUBLIC, ABSTRACT));

    private Set<Object> VARIABLE_MODIFIER =
	new HashSet<> (Arrays.asList ("Annotations", FINAL));
    */

    public Java8Grammar (boolean debug) {
	lr = new LRParser (debug);
    }

    public void buildFullGrammar () {
	addGoal ();
	addAllRules ();
	lr.build ();
    }

    public LRParser getLRParser () {
	return lr;
    }

    public void addGoal () {
	// First rule should be the goal rule
	lr.addRule ("Goal", "CompilationUnit");
    }

    public void addAllRules () {
	addLiteralRules ();
	addTypeRules ();
	addNameRules ();
	addCompilationUnit ();
	addAllClassRules ();
	addInterfaceRules ();
	addAnnotationRules ();
	addArrayInitializer ();
	addBlockStatements ();
	addExpressions ();
    }

    public void addLiteralRules () {
	// Productions from §3 (Lexical Structure)
	lr.addRule ("Literal",
		    lr.oneOf (INT_LITERAL, LONG_LITERAL,
			      FLOAT_LITERAL, DOUBLE_LITERAL,
			      TRUE, FALSE,
			      CHARACTER_LITERAL,
			      STRING_LITERAL,
			      NULL));
    }

    public void addTypeRules () {
	// Productions from §4 (Types, Values, and Variables)
	lr.addRule ("Type", lr.oneOf ("PrimitiveType", "ReferenceType"));
	lr.addRule ("PrimitiveType", lr.zeroOrMore ("Annotation"), lr.oneOf ("NumericType", BOOLEAN));
	lr.addRule ("NumericType", lr.oneOf (BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE));
	// removed TypeVariable, part of ClassOrInterfaceType
	lr.addRule("ReferenceType", lr.oneOf ("ClassType", "ArrayType"));
	lr.addRule ("ClassType",
		    lr.oneOf (lr.sequence ("Annotations", IDENTIFIER, lr.zeroOrOne ("TypeArguments")),
			      lr.sequence ("ClassType", DOT, "Annotations",
					   IDENTIFIER, lr.zeroOrOne ("TypeArguments"))));
	// lr.addRule ("InterfaceType", "ClassType"); removed to avoid conflicts
	// lr.addRule ("TypeVariable", "Annotations", IDENTIFIER);
	lr.addRule ("ArrayType",
		    lr.oneOf (lr.sequence ("PrimitiveType", "Dims"),
			      lr.sequence ("ClassType", "Dims")));
	lr.addRule ("Dims",
		    lr.zeroOrMore ("Annotation"), LEFT_BRACKET, RIGHT_BRACKET,
		    lr.zeroOrMore (lr.zeroOrMore ("Annotation"),  LEFT_BRACKET, RIGHT_BRACKET));
	lr.addRule ("TypeParameter",
		    lr.zeroOrMore ("TypeParameterModifier"), IDENTIFIER, lr.zeroOrOne ("TypeBound"));
	lr.addRule ("TypeParameterModifier", "Annotation");
	lr.addRule ("TypeBound", lr.sequence (EXTENDS, "ClassType", lr.zeroOrMore ("AdditionalBound")));
	lr.addRule ("AdditionalBound", AND, "ClassType");
	lr.addRule ("TypeArguments", LT, "TypeArgumentList", GT);
	lr.addRule ("TypeArgumentList",
		    "TypeArgument", lr.zeroOrMore (COMMA, "TypeArgument"));
	lr.addRule ("TypeArgument", lr.oneOf ("ReferenceType", "Wildcard"));
	lr.addRule ("Wildcard",
		    lr.zeroOrMore ("Annotation"), QUESTIONMARK, lr.zeroOrOne ("WildcardBounds"));
	lr.addRule ("WildcardBounds",
		    lr.oneOf (lr.sequence (EXTENDS, "ReferenceType"),
			      lr.sequence (SUPER, "ReferenceType")));
    }

    // Productions from §8 (Classes)
    public void addAllClassRules () {
	addModifiers ();
	addClassDeclaration ();
	addTypeParameters ();
	addFieldDeclaration ();
	addUnannTypes ();
	addMethodDeclaration ();
	addFormalParameterList ();
	addThrows ();
	addInitializers ();
	addConstructorDeclaration ();
	addEnumDeclaration ();
    }

    public void addClassDeclaration () {
	lr.addRule ("ClassDeclaration",
		    lr.oneOf ("NormalClassDeclaration", "EnumDeclaration"));
	lr.addRule ("NormalClassDeclaration",
		    lr.zeroOrMore ("Modifier"), CLASS, IDENTIFIER,
		    lr.zeroOrOne ("TypeParameters"), lr.zeroOrOne ("Superclass"),
		    lr.zeroOrOne ("Superinterfaces"), "ClassBody");
	lr.addRule ("Superclass", EXTENDS, "ClassType");
	lr.addRule ("Superinterfaces", IMPLEMENTS, "InterfaceTypeList");
	lr.addRule ("InterfaceTypeList", "ClassType", lr.zeroOrMore (COMMA, "ClassType"));
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
    }

    public void addFieldDeclaration () {
	lr.addRule ("FieldDeclaration",
		    lr.zeroOrMore ("Modifier"), "UnannType", "VariableDeclaratorList", SEMICOLON);
	lr.addRule ("VariableDeclaratorList",
		    "VariableDeclarator", lr.zeroOrMore (COMMA, "VariableDeclarator"));
	lr.addRule ("VariableDeclarator",
		    "VariableDeclaratorId", lr.zeroOrOne (EQUAL, "VariableInitializer"));
	lr.addRule ("VariableDeclaratorId",
		    IDENTIFIER, lr.zeroOrOne ("Dims"));
	lr.addRule ("VariableInitializer", lr.oneOf ("Expression", "ArrayInitializer"));
    }

    public void addTypeParameters () {
	lr.addRule ("TypeParameters",
		    LT, "TypeParameterList", GT);
	lr.addRule ("TypeParameterList",
		    "TypeParameter", lr.zeroOrMore (COMMA, "TypeParameter"));
    }

    public void addMethodDeclaration () {
	lr.addRule ("MethodDeclaration",
		    lr.zeroOrMore ("Modifier"), "MethodHeader", "MethodBody");
	lr.addRule ("MethodHeader",
		    lr.zeroOrOne ("TypeParameters", lr.zeroOrMore ("Annotation")),
		    lr.oneOf ("UnannType", VOID), "MethodDeclarator", lr.zeroOrOne ("Throws"));
	lr.addRule ("MethodDeclarator",
		    IDENTIFIER, LEFT_PARENTHESIS, lr.zeroOrOne ("FormalParameterList"), RIGHT_PARENTHESIS,
		    lr.zeroOrOne ("Dims"));
	lr.addRule ("MethodBody", lr.oneOf ("Block", SEMICOLON));
    }

    public void addInitializers () {
	lr.addRule ("InstanceInitializer", "Block");
	lr.addRule ("StaticInitializer", STATIC, "Block");
    }

    public void addConstructorDeclaration () {
	lr.addRule ("ConstructorDeclaration",
		    lr.zeroOrMore ("Modifier"), "ConstructorDeclarator",
		    lr.zeroOrOne ("Throws"), "ConstructorBody");
	lr.addRule ("ConstructorDeclarator",
		    lr.zeroOrOne ("TypeParameters"), IDENTIFIER,
		    LEFT_PARENTHESIS, lr.zeroOrOne ("FormalParameterList"), RIGHT_PARENTHESIS);
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
			      lr.sequence ("ComplexName", DOT,
					   lr.zeroOrOne ("TypeArguments"), SUPER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   SEMICOLON),
			      lr.sequence ("Primary", DOT,
					   lr.zeroOrOne ("TypeArguments"), SUPER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   SEMICOLON)));
    }

    public void addFormalParameterList () {
	lr.addRule ("FormalParameterList",
		    lr.oneOf (lr.sequence ("ReceiverParameter", "FormalParameterListRest"),
			      lr.sequence ("FormalParameter", "FormalParameterListRest"),
			      "LastFormalParameter"));
	lr.addRule ("FormalParameter",
		    lr.zeroOrMore ("VariableModifier"), "UnannType", "VariableDeclaratorId");
	lr.addRule ("FormalParameter",
		    lr.zeroOrMore ("Annotation"), "UnannType", "VariableDeclaratorId");
	lr.addRule ("VariableModifier", lr.oneOf ("Annotations", FINAL));
	lr.addRule ("LastFormalParameter",
		    lr.oneOf (lr.sequence (lr.zeroOrMore ("VariableModifier"),
					   "UnannType", lr.zeroOrMore ("Annotation"),
					   ELLIPSIS, "VariableDeclaratorId"),
			      lr.sequence (lr.zeroOrMore ("Annotation"),
					   "UnannType", lr.zeroOrMore ("Annotation"),
					   ELLIPSIS, "VariableDeclaratorId")));
	lr.addRule ("ReceiverParameter",
		    lr.zeroOrMore ("Annotation"), "UnannType", lr.zeroOrOne (IDENTIFIER, DOT), THIS);
	lr.addRule ("FormalParameterListRest",
		    lr.sequence (lr.zeroOrMore (COMMA, "FormalParameter"),
				 lr.zeroOrOne (COMMA, "LastFormalParameter")));
    }

    public void addThrows () {
	lr.addRule ("Throws",
		    THROWS, "ExceptionTypeList");
	lr.addRule ("ExceptionTypeList",
		    "ExceptionType", lr.zeroOrMore (COMMA, "ExceptionType"));
	lr.addRule ("ExceptionType", "ClassType");
    }

    public void addEnumDeclaration () {
	lr.addRule ("EnumDeclaration",
		    lr.zeroOrMore ("Modifier"),
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
    }

    // Productions from §9 (Interfaces)
    public void addInterfaceRules () {
	lr.addRule ("InterfaceDeclaration",
		    lr.oneOf ("NormalInterfaceDeclaration", "AnnotationTypeDeclaration"));
	lr.addRule ("NormalInterfaceDeclaration",
		    lr.zeroOrMore ("Modifier"), INTERFACE, IDENTIFIER,
		    lr.zeroOrOne ("TypeParameters"), lr.zeroOrOne ("ExtendsInterfaces"), "InterfaceBody");
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
		    lr.zeroOrMore ("Modifier"), "UnannType", "VariableDeclaratorList", SEMICOLON);
	lr.addRule ("InterfaceMethodDeclaration",
		    lr.zeroOrMore (lr.oneOf ("Modifier", DEFAULT)), "MethodHeader", "MethodBody");
	lr.addRule ("AnnotationTypeDeclaration",
		    lr.zeroOrMore ("Modifier"),
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
		    lr.zeroOrMore ("Modifier"), "UnannType",
		    IDENTIFIER, LEFT_PARENTHESIS, RIGHT_PARENTHESIS, lr.zeroOrOne ("Dims"),
		    lr.zeroOrOne ("DefaultValue"), SEMICOLON);
	lr.addRule ("DefaultValue",
		    DEFAULT, "ElementValue");
    }

    public void addUnannTypes () {
	lr.addRule ("UnannType", lr.oneOf ("NumericType", "UnannReferenceType"));
	/* UnannClassOrInterfaceType has been removed */
	lr.addRule ("UnannReferenceType", lr.oneOf ("UnannClassType", "UnannArrayType"));

	// really lr.oneOf ("UnannClassType", "UnannInterfaceType");
	// lr.addRule ("UnannClassOrInterfaceType", "UnannClassType");
	// UnannTypeVariable: IDENTIFIER
	lr.addRule ("UnannClassType",
		    lr.zeroOrOne ("UnannClassType", DOT, "Annotations"),
		    IDENTIFIER, lr.zeroOrOne ("TypeArguments"));
	lr.addRule ("UnannArrayType",
		    lr.oneOf ("NumericType", BOOLEAN, "UnannClassType"), "Dims");
    }

    // Productions from §6 Names
    public void addNameRules () {
	lr.addRule ("ComplexName",
		    lr.oneOf (IDENTIFIER,
			      lr.sequence ("ComplexName", DOT, IDENTIFIER)));
    }

    // Productions from §7 (Packages)
    public void addCompilationUnit () {
	lr.addRule ("CompilationUnit",
		    lr.zeroOrOne ("PackageDeclaration"),
		    lr.zeroOrMore ("ImportDeclaration"),
		    lr.zeroOrMore ("TypeDeclaration"));
	addPackageRules ();
	addImportRules ();
	addTypeDeclaration ();
    }

    public void addPackageRules () {
	lr.addRule ("PackageDeclaration",
		    lr.zeroOrMore ("PackageModifier"), PACKAGE, IDENTIFIER,
		    lr.zeroOrMore (DOT, IDENTIFIER), SEMICOLON);
	lr.addRule ("PackageModifier", "Annotation");
    }

    public void addImportRules () {
	lr.addRule ("ImportDeclaration",
		    lr.oneOf ("SingleTypeImportDeclaration",
			      "TypeImportOnDemandDeclaration",
			      "SingleStaticImportDeclaration",
			      "StaticImportOnDemandDeclaration"));
	lr.addRule ("SingleTypeImportDeclaration",
		    IMPORT, "ComplexName", SEMICOLON);
	// This one should really be IMPORT, PackageOrTypeName, but then we need more lookahead
	lr.addRule ("TypeImportOnDemandDeclaration",
		    IMPORT, "ComplexName", DOT, MULTIPLY, SEMICOLON);
	lr.addRule ("SingleStaticImportDeclaration",
		    IMPORT, STATIC, "ComplexName", DOT, IDENTIFIER, SEMICOLON);
	lr.addRule ("StaticImportOnDemandDeclaration",
		    IMPORT, STATIC, "ComplexName", DOT, MULTIPLY, SEMICOLON);
    }

    public void addTypeDeclaration () {
	lr.addRule ("TypeDeclaration",
		    lr.oneOf ("ClassDeclaration", "InterfaceDeclaration", SEMICOLON));
    }

    public void addAnnotationRules () {
	lr.addRule ("Annotations", lr.zeroOrMore ("Annotation"));
	lr.addRule ("Annotation",
		    lr.oneOf ("NormalAnnotation", "MarkerAnnotation", "SingleElementAnnotation"));
	lr.addRule ("NormalAnnotation",
		    AT, "ComplexName", LEFT_PARENTHESIS, lr.zeroOrOne ("ElementValuePairList"), RIGHT_PARENTHESIS);
	lr.addRule ("ElementValuePairList",
		    "ElementValuePair", lr.zeroOrMore (COMMA, "ElementValuePair"));
	lr.addRule ("ElementValuePair",
		    IDENTIFIER, EQUAL, "ElementValue");
	lr.addRule ("ElementValue",
		    lr.oneOf ("ConditionalExpression",
			      "ElementValueArrayInitializer",
			      "Annotation"));
	lr.addRule ("ElementValueArrayInitializer",
		    LEFT_CURLY, lr.zeroOrOne ("ElementValueList"), lr.zeroOrOne (COMMA), RIGHT_CURLY);
	lr.addRule ("ElementValueList",
		    "ElementValue", lr.zeroOrMore (COMMA, "ElementValue"));
	lr.addRule ("MarkerAnnotation",
		    AT, "ComplexName");
	lr.addRule ("SingleElementAnnotation",
		    AT, "ComplexName", LEFT_PARENTHESIS, "ElementValue", RIGHT_PARENTHESIS);
    }

    // Productions from §10 (Arrays)
    public void addArrayInitializer () {
	lr.addRule ("ArrayInitializer",
		    LEFT_CURLY, lr.zeroOrOne ("VariableInitializerList"),
		    lr.zeroOrOne (COMMA), RIGHT_CURLY);
	lr.addRule ("VariableInitializerList",
		    "VariableInitializer", lr.zeroOrMore (COMMA, "VariableInitializer"));
    }

    // Productions from §14 (Blocks and Statements)
    public void addBlockStatements () {
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
		    lr.sequence ("SwitchLabel", lr.zeroOrMore ("SwitchLabel"), "BlockStatements"));
	lr.addRule ("SwitchLabel",
		    lr.oneOf (lr.sequence (CASE, "ConstantExpression", COLON),
			      lr.sequence (CASE, IDENTIFIER, COLON),
			      lr.sequence (DEFAULT, COLON)));
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
    }

    // Productions from §15 (Expressions)
    public void addExpressions () {
	lr.addRule ("Primary", lr.oneOf ("PrimaryNoNewArray", "ArrayCreationExpression"));
	lr.addRule ("PrimaryNoNewArray",
		    lr.oneOf ("Literal",
			      lr.sequence ("ComplexName", lr.zeroOrMore (LEFT_BRACKET, RIGHT_BRACKET), DOT, CLASS),
			      lr.sequence (VOID, DOT, CLASS),
			      THIS,
			      lr.sequence ("ComplexName", DOT, THIS),
			      lr.sequence (LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS),
			      "ClassInstanceCreationExpression",
			      "FieldAccess",
			      "ArrayAccess",
			      "MethodInvocation",
			      "MethodReference"));
	lr.addRule ("ClassInstanceCreationExpression",
		    lr.oneOf (lr.sequence (lr.zeroOrOne (lr.oneOf ("ComplexName", "Primary"), DOT),
					   NEW, lr.zeroOrOne ("TypeArguments"),
					   lr.zeroOrMore ("Annotation"), IDENTIFIER,
					   lr.zeroOrOne ("TypeArgumentsOrDiamond"),
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
					   lr.zeroOrOne ("ClassBody"))));
	lr.addRule ("TypeArgumentsOrDiamond",
		    lr.oneOf ("TypeArguments", lr.sequence (LT, GT)));
	lr.addRule ("FieldAccess",
		    lr.oneOf (lr.sequence ("Primary", DOT, IDENTIFIER),
			      lr.sequence (SUPER, DOT, IDENTIFIER),
			      lr.sequence ("ComplexName", DOT, SUPER, DOT, IDENTIFIER)));
	lr.addRule ("ArrayAccess",
		    lr.oneOf (lr.sequence ("ComplexName", LEFT_BRACKET, "Expression", RIGHT_BRACKET),
			      lr.sequence ("PrimaryNoNewArray", LEFT_BRACKET, "Expression", RIGHT_BRACKET)));
	lr.addRule ("MethodInvocation",
		    lr.oneOf (lr.sequence (IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence ("ComplexName", DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence ("ComplexName", DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence ("Primary", DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence (SUPER, DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      lr.sequence ("ComplexName", DOT, SUPER, DOT, lr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, lr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS)));
	lr.addRule ("ArgumentList",
		    "Expression", lr.zeroOrMore (COMMA, "Expression"));
	lr.addRule ("MethodReference",
		    lr.oneOf (lr.sequence ("ComplexName", DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence ("ReferenceType", DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence ("Primary", DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence (SUPER, DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence ("ComplexName", DOT, SUPER, DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      lr.sequence ("ClassType", DOUBLE_COLON,
					   lr.zeroOrOne ("TypeArguments"), NEW),
			      lr.sequence ("ArrayType", DOUBLE_COLON, NEW)));
	lr.addRule ("ArrayCreationExpression",
		    lr.oneOf (lr.sequence (NEW, "PrimitiveType", "DimExprs", lr.zeroOrOne ("Dims")),
			      lr.sequence (NEW, "ClassType", "DimExprs", lr.zeroOrOne ("Dims")),
			      lr.sequence (NEW, "PrimitiveType", "Dims", "ArrayInitializer"),
			      lr.sequence (NEW, "ClassType", "Dims", "ArrayInitializer")));
	lr.addRule ("DimExprs", "DimExpr", lr.zeroOrMore ("DimExpr"));
	lr.addRule ("DimExpr",
		    lr.zeroOrMore ("Annotation"), LEFT_BRACKET, "Expression", RIGHT_BRACKET);
	lr.addRule ("ConstantExpression", "Expression");
	lr.addRule ("Expression", lr.oneOf ("LambdaExpression", "AssignmentExpression"));
	lr.addRule ("LambdaExpression",
		    lr.oneOf (lr.sequence (lr.oneOf (IDENTIFIER, "LambdaParameters"),
					   ARROW, "LambdaBody")));
	lr.addRule ("LambdaParameters",
		    lr.oneOf (lr.sequence (LEFT_PARENTHESIS,
					   lr.zeroOrOne ("FormalParameterList"),
					   RIGHT_PARENTHESIS),
			      lr.sequence (LEFT_PARENTHESIS,
					   IDENTIFIER, lr.zeroOrMore (COMMA, IDENTIFIER),
					   RIGHT_PARENTHESIS)));
	lr.addRule ("LambdaBody", lr.oneOf ("Expression", "Block"));
	lr.addRule ("AssignmentExpression", lr.oneOf ("ConditionalExpression", "Assignment"));
	lr.addRule ("Assignment",
		    lr.sequence (lr.oneOf ("ComplexName", "FieldAccess", "ArrayAccess"),
				 "AssignmentOperator", "Expression"));
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
			      "ComplexName",
			      "PostIncrementExpression",
			      "PostDecrementExpression"));
	lr.addRule ("PostIncrementExpression",
		    "PostfixExpression", INCREMENT);
	lr.addRule ("PostDecrementExpression",
		    "PostfixExpression", DECREMENT);
	lr.addRule ("CastExpression",
		    lr.oneOf (lr.sequence (LEFT_PARENTHESIS,
					   lr.zeroOrMore ("Annotation"), lr.oneOf ("NumericType", BOOLEAN),
					   RIGHT_PARENTHESIS,
					   "UnaryExpression"),
			      lr.sequence (LEFT_PARENTHESIS,
					   "ReferenceType", lr.zeroOrMore ("AdditionalBound"),
					   RIGHT_PARENTHESIS, "UnaryExpressionNotPlusMinus"),
			      lr.sequence (LEFT_PARENTHESIS,
					   "ReferenceType", lr.zeroOrMore ("AdditionalBound"),
					   RIGHT_PARENTHESIS, "LambdaExpression")));
    }

    public void addModifiers () {
	lr.addRule ("Modifier",
		    lr.oneOf ("Annotation",
			      PUBLIC, PROTECTED, PRIVATE,
			      ABSTRACT, STATIC, FINAL, STRICTFP, TRANSIENT,
			      VOLATILE, SYNCHRONIZED, NATIVE));
    }
}