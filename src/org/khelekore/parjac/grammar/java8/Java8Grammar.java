package org.khelekore.parjac.grammar.java8;

import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.parser.LRParser;

import static org.khelekore.parjac.lexer.Token.*;

public class Java8Grammar {
    private final Grammar gr;
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
	gr = new Grammar ();
	lr = new LRParser (gr, debug);
    }

    public void buildFullGrammar () {
	addGoal ();
	addAllRules ();
    }

    public Grammar getGrammar () {
	return gr;
    }

    public void addGoal () {
	// First rule should be the goal rule
	gr.addRule ("Goal", "CompilationUnit");
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
	gr.addRule ("Literal",
		    gr.oneOf (INT_LITERAL, LONG_LITERAL,
			      FLOAT_LITERAL, DOUBLE_LITERAL,
			      TRUE, FALSE,
			      CHARACTER_LITERAL,
			      STRING_LITERAL,
			      NULL));
    }

    public void addTypeRules () {
	// Productions from §4 (Types, Values, and Variables)
	gr.addRule ("Type", gr.oneOf ("PrimitiveType", "ReferenceType"));
	gr.addRule ("PrimitiveType", gr.zeroOrMore ("Annotation"), gr.oneOf ("NumericType", BOOLEAN));
	gr.addRule ("NumericType", gr.oneOf (BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE));
	// removed TypeVariable, part of ClassOrInterfaceType
	gr.addRule("ReferenceType", gr.oneOf ("ClassType", "ArrayType"));
	gr.addRule ("ClassType",
		    gr.zeroOrOne ("ClassType", DOT), gr.zeroOrMore ("Annotation"),
		    "TypedName");
	gr.addRule ("TypedName", IDENTIFIER, gr.zeroOrOne ("TypeArguments"));
	gr.addRule ("NonTrivialClassType",
		    "ClassType", DOT, gr.zeroOrMore ("Annotation"),
		    "TypedName");
	// gr.addRule ("InterfaceType", "ClassType"); removed to avoid conflicts
	// gr.addRule ("TypeVariable", "Annotations", IDENTIFIER);
	gr.addRule ("ArrayType",
		    gr.oneOf ("PrimitiveType", "ClassType"), "Dims");
	gr.addRule ("Dims",
		    gr.zeroOrMore ("Annotation"), LEFT_BRACKET, RIGHT_BRACKET,
		    gr.zeroOrMore (gr.zeroOrMore ("Annotation"),  LEFT_BRACKET, RIGHT_BRACKET));
	gr.addRule ("TypeParameter",
		    gr.zeroOrMore ("TypeParameterModifier"), IDENTIFIER, gr.zeroOrOne ("TypeBound"));
	gr.addRule ("TypeParameterModifier", "Annotation");
	gr.addRule ("TypeBound", EXTENDS, "ClassType", gr.zeroOrMore ("AdditionalBound"));
	gr.addRule ("AdditionalBound", AND, "ClassType");
	gr.addRule ("TypeArguments", LT, "TypeArgumentList", GT);
	gr.addRule ("TypeArgumentList",
		    "TypeArgument", gr.zeroOrMore (COMMA, "TypeArgument"));
	gr.addRule ("TypeArgument", gr.oneOf ("ReferenceType", "Wildcard"));
	gr.addRule ("Wildcard",
		    gr.zeroOrMore ("Annotation"), QUESTIONMARK, gr.zeroOrOne ("WildcardBounds"));
	gr.addRule ("WildcardBounds",
		    gr.oneOf (gr.sequence (EXTENDS, "ReferenceType"),
			      gr.sequence (SUPER, "ReferenceType")));
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
	gr.addRule ("ClassDeclaration",
		    gr.oneOf ("NormalClassDeclaration", "EnumDeclaration"));
	gr.addRule ("NormalClassDeclaration",
		    gr.zeroOrMore ("Modifier"), CLASS, IDENTIFIER,
		    gr.zeroOrOne ("TypeParameters"), gr.zeroOrOne ("Superclass"),
		    gr.zeroOrOne ("Superinterfaces"), "ClassBody");
	gr.addRule ("Superclass", EXTENDS, "ClassType");
	gr.addRule ("Superinterfaces", IMPLEMENTS, "InterfaceTypeList");
	gr.addRule ("InterfaceTypeList", "ClassType", gr.zeroOrMore (COMMA, "ClassType"));
	gr.addRule ("ClassBody",
		    LEFT_CURLY,
		    gr.zeroOrMore ("ClassBodyDeclaration"),
		    RIGHT_CURLY);
	gr.addRule ("ClassBodyDeclaration",
		    gr.oneOf ("ClassMemberDeclaration",
			      "InstanceInitializer",
			      "StaticInitializer",
			      "ConstructorDeclaration"));
	gr.addRule ("ClassMemberDeclaration",
		    gr.oneOf ("FieldDeclaration",
			      "MethodDeclaration",
			      "ClassDeclaration",
			      "InterfaceDeclaration",
			      SEMICOLON));
    }

    public void addFieldDeclaration () {
	gr.addRule ("FieldDeclaration",
		    gr.zeroOrMore ("Modifier"), "UnannType", "VariableDeclaratorList", SEMICOLON);
	gr.addRule ("VariableDeclaratorList",
		    "VariableDeclarator", gr.zeroOrMore (COMMA, "VariableDeclarator"));
	gr.addRule ("VariableDeclarator",
		    "VariableDeclaratorId", gr.zeroOrOne (EQUAL, "VariableInitializer"));
	gr.addRule ("VariableDeclaratorId",
		    IDENTIFIER, gr.zeroOrOne ("Dims"));
	gr.addRule ("VariableInitializer", gr.oneOf ("Expression", "ArrayInitializer"));
    }

    public void addTypeParameters () {
	gr.addRule ("TypeParameters",
		    LT, "TypeParameterList", GT);
	gr.addRule ("TypeParameterList",
		    "TypeParameter", gr.zeroOrMore (COMMA, "TypeParameter"));
    }

    public void addMethodDeclaration () {
	gr.addRule ("MethodDeclaration",
		    gr.zeroOrMore ("Modifier"), "MethodHeader", "MethodBody");
	gr.addRule ("MethodHeader",
		    gr.zeroOrOne ("TypeParameters", gr.zeroOrMore ("Annotation")),
		    gr.oneOf ("UnannType", VOID), "MethodDeclarator", gr.zeroOrOne ("Throws"));
	gr.addRule ("MethodDeclarator",
		    IDENTIFIER, LEFT_PARENTHESIS, gr.zeroOrOne ("FormalParameterList"), RIGHT_PARENTHESIS,
		    gr.zeroOrOne ("Dims"));
	gr.addRule ("MethodBody", gr.oneOf ("Block", SEMICOLON));
    }

    public void addInitializers () {
	gr.addRule ("InstanceInitializer", "Block");
	gr.addRule ("StaticInitializer", STATIC, "Block");
    }

    public void addConstructorDeclaration () {
	gr.addRule ("ConstructorDeclaration",
		    gr.zeroOrMore ("Modifier"), "ConstructorDeclarator",
		    gr.zeroOrOne ("Throws"), "ConstructorBody");
	gr.addRule ("ConstructorDeclarator",
		    gr.zeroOrOne ("TypeParameters"), IDENTIFIER,
		    LEFT_PARENTHESIS, gr.zeroOrOne ("FormalParameterList"), RIGHT_PARENTHESIS);
	gr.addRule ("ConstructorBody",
		    LEFT_CURLY,
		    gr.zeroOrOne ("ExplicitConstructorInvocation"), gr.zeroOrOne ("BlockStatements"),
		    RIGHT_CURLY);
	gr.addRule ("ExplicitConstructorInvocation",
		    gr.oneOf (gr.sequence (gr.zeroOrOne ("TypeArguments"), THIS),
			      gr.sequence (gr.zeroOrOne (gr.oneOf (IDENTIFIER, "MultiName", "Primary"), DOT),
					   gr.zeroOrOne ("TypeArguments"), SUPER)),
		    LEFT_PARENTHESIS, gr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS, SEMICOLON);
    }

    public void addFormalParameterList () {
	gr.addRule ("FormalParameterList",
		    gr.oneOf (gr.sequence ("ReceiverParameter", "FormalParameterListRest"),
			      gr.sequence ("FormalParameter", "FormalParameterListRest"),
			      "LastFormalParameter"));
	gr.addRule ("FormalParameter",
		    gr.oneOf (gr.zeroOrOne ("VariableModifiers"), gr.zeroOrMore ("Annotation")),
		    "UnannType", "VariableDeclaratorId");
	gr.addRule ("VariableModifiers", gr.zeroOrMore ("Annotation"), FINAL, gr.zeroOrMore ("Annotation"));
	gr.addRule ("LastFormalParameter",
		    gr.oneOf (gr.zeroOrOne ("VariableModifiers"), gr.zeroOrMore ("Annotation")),
		    "UnannType", gr.zeroOrMore ("Annotation"), ELLIPSIS, "VariableDeclaratorId");
	gr.addRule ("ReceiverParameter",
		    gr.zeroOrMore ("Annotation"), "UnannType", gr.zeroOrOne (IDENTIFIER, DOT), THIS);
	gr.addRule ("FormalParameterListRest",
		    gr.sequence (gr.zeroOrMore (COMMA, "FormalParameter"),
				 gr.zeroOrOne (COMMA, "LastFormalParameter")));
    }

    public void addThrows () {
	gr.addRule ("Throws",
		    THROWS, "ExceptionTypeList");
	gr.addRule ("ExceptionTypeList",
		    "ExceptionType", gr.zeroOrMore (COMMA, "ExceptionType"));
	gr.addRule ("ExceptionType", "ClassType");
    }

    public void addEnumDeclaration () {
	gr.addRule ("EnumDeclaration",
		    gr.zeroOrMore ("Modifier"),
		    ENUM, IDENTIFIER, gr.zeroOrOne ("Superinterfaces"), "EnumBody");
	gr.addRule ("EnumBody",
		    LEFT_CURLY,
		    gr.zeroOrOne ("EnumConstantList"), gr.zeroOrOne (COMMA),
		    gr.zeroOrOne ("EnumBodyDeclarations"),
		    RIGHT_CURLY);
	gr.addRule ("EnumConstantList",
		    "EnumConstant", gr.zeroOrMore (COMMA, "EnumConstant"));
	gr.addRule ("EnumConstant",
		    gr.zeroOrMore ("EnumConstantModifier"), IDENTIFIER,
		    gr.zeroOrOne (LEFT_PARENTHESIS, gr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
		    gr.zeroOrOne ("ClassBody"));
	gr.addRule ("EnumConstantModifier",
		    "Annotation");
	gr.addRule ("EnumBodyDeclarations",
		    SEMICOLON, gr.zeroOrMore ("ClassBodyDeclaration"));
    }

    // Productions from §9 (Interfaces)
    public void addInterfaceRules () {
	gr.addRule ("InterfaceDeclaration",
		    gr.oneOf ("NormalInterfaceDeclaration", "AnnotationTypeDeclaration"));
	gr.addRule ("NormalInterfaceDeclaration",
		    gr.zeroOrMore ("Modifier"), INTERFACE, IDENTIFIER,
		    gr.zeroOrOne ("TypeParameters"), gr.zeroOrOne ("ExtendsInterfaces"), "InterfaceBody");
	gr.addRule ("ExtendsInterfaces",
		    EXTENDS, "InterfaceTypeList");
	gr.addRule ("InterfaceBody",
		    LEFT_CURLY,
		    gr.zeroOrMore ("InterfaceMemberDeclaration"),
		    RIGHT_CURLY);
	gr.addRule ("InterfaceMemberDeclaration",
		    gr.oneOf ("ConstantDeclaration",
			      "InterfaceMethodDeclaration",
			      "ClassDeclaration",
			      "InterfaceDeclaration",
			      SEMICOLON));
	gr.addRule ("ConstantDeclaration",
		    gr.zeroOrMore ("Modifier"), "UnannType", "VariableDeclaratorList", SEMICOLON);
	gr.addRule ("InterfaceMethodDeclaration",
		    gr.zeroOrMore ("Modifier"), gr.zeroOrOne (DEFAULT), gr.zeroOrMore ("Modifier"),
		    "MethodHeader", "MethodBody");
	gr.addRule ("AnnotationTypeDeclaration",
		    gr.zeroOrMore ("Modifier"),
		    AT, INTERFACE, IDENTIFIER, "AnnotationTypeBody");
	gr.addRule ("AnnotationTypeBody",
		    LEFT_CURLY,
		    gr.zeroOrMore ("AnnotationTypeMemberDeclaration"),
		    RIGHT_CURLY);
	gr.addRule ("AnnotationTypeMemberDeclaration",
		    gr.oneOf ("AnnotationTypeElementDeclaration",
			      "ConstantDeclaration",
			      "ClassDeclaration",
			      "InterfaceDeclaration",
			      SEMICOLON));
	gr.addRule ("AnnotationTypeElementDeclaration",
		    gr.zeroOrMore ("Modifier"), "UnannType",
		    IDENTIFIER, LEFT_PARENTHESIS, RIGHT_PARENTHESIS, gr.zeroOrOne ("Dims"),
		    gr.zeroOrOne ("DefaultValue"), SEMICOLON);
	gr.addRule ("DefaultValue",
		    DEFAULT, "ElementValue");
    }

    public void addUnannTypes () {
	gr.addRule ("UnannType", gr.oneOf ("NumericType", BOOLEAN, "UnannClassType", "UnannArrayType"));

	// really gr.oneOf ("UnannClassType", "UnannInterfaceType");
	// gr.addRule ("UnannClassOrInterfaceType", "UnannClassType");
	// UnannTypeVariable: IDENTIFIER
	gr.addRule ("UnannClassType",
		    gr.zeroOrOne ("UnannClassType", DOT, gr.zeroOrMore ("Annotation")),
		    "TypedName");
	gr.addRule ("UnannArrayType",
		    gr.oneOf ("NumericType", BOOLEAN, "UnannClassType"), "Dims");
    }

    // Productions from §6 Names
    public void addNameRules () {
	gr.addRule ("ComplexName",
		    gr.oneOf (IDENTIFIER, gr.sequence ("ComplexName", DOT, IDENTIFIER)));
	gr.addRule ("MultiName",
		    gr.oneOf (gr.sequence (IDENTIFIER, DOT, IDENTIFIER),
			      gr.sequence ("MultiName", DOT, IDENTIFIER)));
    }

    // Productions from §7 (Packages)
    public void addCompilationUnit () {
	gr.addRule ("CompilationUnit",
		    gr.zeroOrOne ("PackageDeclaration"),
		    gr.zeroOrMore ("ImportDeclaration"),
		    gr.zeroOrMore ("TypeDeclaration"));
	addPackageRules ();
	addImportRules ();
	addTypeDeclaration ();
    }

    public void addPackageRules () {
	gr.addRule ("PackageDeclaration",
		    gr.zeroOrMore ("Annotation"), PACKAGE, IDENTIFIER,
		    gr.zeroOrMore (DOT, IDENTIFIER), SEMICOLON);
    }

    public void addImportRules () {
	gr.addRule ("ImportDeclaration",
		    gr.oneOf ("SingleTypeImportDeclaration",
			      "TypeImportOnDemandDeclaration",
			      "SingleStaticImportDeclaration",
			      "StaticImportOnDemandDeclaration"));
	gr.addRule ("SingleTypeImportDeclaration",
		    IMPORT, "ComplexName", SEMICOLON);
	// This one should really be IMPORT, PackageOrTypeName, but then we need more lookahead
	gr.addRule ("TypeImportOnDemandDeclaration",
		    IMPORT, "ComplexName", DOT, MULTIPLY, SEMICOLON);
	gr.addRule ("SingleStaticImportDeclaration",
		    IMPORT, STATIC, "ComplexName", DOT, IDENTIFIER, SEMICOLON);
	gr.addRule ("StaticImportOnDemandDeclaration",
		    IMPORT, STATIC, "ComplexName", DOT, MULTIPLY, SEMICOLON);
    }

    public void addTypeDeclaration () {
	gr.addRule ("TypeDeclaration",
		    gr.oneOf ("ClassDeclaration", "InterfaceDeclaration", SEMICOLON));
    }

    public void addAnnotationRules () {
	gr.addRule ("Annotation",
		    gr.oneOf ("NormalAnnotation", "MarkerAnnotation", "SingleElementAnnotation"));
	gr.addRule ("NormalAnnotation",
		    AT, "ComplexName", LEFT_PARENTHESIS, gr.zeroOrOne ("ElementValuePairList"), RIGHT_PARENTHESIS);
	gr.addRule ("ElementValuePairList",
		    "ElementValuePair", gr.zeroOrMore (COMMA, "ElementValuePair"));
	gr.addRule ("ElementValuePair",
		    IDENTIFIER, EQUAL, "ElementValue");
	gr.addRule ("ElementValue",
		    gr.oneOf ("ConditionalExpression", "ElementValueArrayInitializer", "Annotation"));
	gr.addRule ("ElementValueArrayInitializer",
		    LEFT_CURLY, gr.zeroOrOne ("ElementValueList"), gr.zeroOrOne (COMMA), RIGHT_CURLY);
	gr.addRule ("ElementValueList", "ElementValue", gr.zeroOrMore (COMMA, "ElementValue"));
	gr.addRule ("MarkerAnnotation", AT, "ComplexName");
	gr.addRule ("SingleElementAnnotation",
		    AT, "ComplexName", LEFT_PARENTHESIS, "ElementValue", RIGHT_PARENTHESIS);
    }

    // Productions from §10 (Arrays)
    public void addArrayInitializer () {
	gr.addRule ("ArrayInitializer",
		    LEFT_CURLY, gr.zeroOrOne ("VariableInitializerList"),
		    gr.zeroOrOne (COMMA), RIGHT_CURLY);
	gr.addRule ("VariableInitializerList",
		    "VariableInitializer", gr.zeroOrMore (COMMA, "VariableInitializer"));
    }

    // Productions from §14 (Blocks and Statements)
    public void addBlockStatements () {
	gr.addRule ("Block",
		    LEFT_CURLY, gr.zeroOrOne ("BlockStatements"), RIGHT_CURLY);
	gr.addRule ("BlockStatements",
		    "BlockStatement", gr.zeroOrMore ("BlockStatement"));
	gr.addRule ("BlockStatement",
		    gr.oneOf ("LocalVariableDeclarationStatement",
			      "ClassDeclaration",
			      "Statement"));
	gr.addRule ("LocalVariableDeclarationStatement", "LocalVariableDeclaration", SEMICOLON);
	gr.addRule ("LocalVariableDeclaration",
		    gr.oneOf (gr.zeroOrOne ("VariableModifiers"), gr.zeroOrMore ("Annotation")),
		    "UnannType", "VariableDeclaratorList");
	gr.addRule ("Statement",
		    gr.oneOf ("StatementWithoutTrailingSubstatement",
			      "LabeledStatement",
			      "IfThenStatement",
			      "IfThenElseStatement",
			      "WhileStatement",
			      "ForStatement"));
	gr.addRule ("StatementNoShortIf",
		    gr.oneOf ("StatementWithoutTrailingSubstatement",
			      "LabeledStatementNoShortIf",
			      "IfThenElseStatementNoShortIf",
			      "WhileStatementNoShortIf",
			      "ForStatementNoShortIf"));
	gr.addRule ("StatementWithoutTrailingSubstatement",
		    gr.oneOf ("Block",
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
	gr.addRule ("EmptyStatement", SEMICOLON);
	gr.addRule ("LabeledStatement", IDENTIFIER, COLON, "Statement");
	gr.addRule ("LabeledStatementNoShortIf", IDENTIFIER, COLON, "StatementNoShortIf");
	gr.addRule ("ExpressionStatement", "StatementExpression", SEMICOLON);
	gr.addRule ("StatementExpression",
		    gr.oneOf ("Assignment",
			      "PreIncrementExpression",
			      "PreDecrementExpression",
			      "PostIncrementExpression",
			      "PostDecrementExpression",
			      "MethodInvocation",
			      "ClassInstanceCreationExpression"));
	gr.addRule ("IfThenStatement",
		    IF, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "Statement");
	gr.addRule ("IfThenElseStatement",
		    IF, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS,
		    "StatementNoShortIf", ELSE, "Statement");
	gr.addRule ("IfThenElseStatementNoShortIf",
		    IF, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS,
		    "StatementNoShortIf", ELSE, "StatementNoShortIf");
	gr.addRule ("AssertStatement",
		    gr.oneOf (gr.sequence (ASSERT, "Expression", SEMICOLON),
			      gr.sequence (ASSERT, "Expression", COLON, "Expression", SEMICOLON)));
	gr.addRule ("SwitchStatement",
		    SWITCH, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "SwitchBlock");
	gr.addRule ("SwitchBlock",
		    LEFT_CURLY, gr.zeroOrMore ("SwitchBlockStatementGroup"),
		    gr.zeroOrMore ("SwitchLabel"), RIGHT_CURLY);
	gr.addRule ("SwitchBlockStatementGroup",
		    "SwitchLabel", gr.zeroOrMore ("SwitchLabel"), "BlockStatements");
	gr.addRule ("SwitchLabel",
		    gr.oneOf (gr.sequence (CASE, "ConstantExpression", COLON),
			      gr.sequence (CASE, IDENTIFIER, COLON),
			      gr.sequence (DEFAULT, COLON)));
	gr.addRule ("WhileStatement",
		    WHILE, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "Statement");
	gr.addRule ("WhileStatementNoShortIf",
		    WHILE, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "StatementNoShortIf");
	gr.addRule ("DoStatement",
		    DO, "Statement", WHILE, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, SEMICOLON);
	gr.addRule ("ForStatement", gr.oneOf ("BasicForStatement", "EnhancedForStatement"));
	gr.addRule ("ForStatementNoShortIf",
		    gr.oneOf ("BasicForStatementNoShortIf",
			      "EnhancedForStatementNoShortIf"));
	gr.addRule ("BasicForStatement",
		    FOR, LEFT_PARENTHESIS,
		    gr.zeroOrOne ("ForInit"), SEMICOLON,
		    gr.zeroOrOne ("Expression"), SEMICOLON,
		    gr.zeroOrOne ("ForUpdate"),
		    RIGHT_PARENTHESIS, "Statement");
	gr.addRule ("BasicForStatementNoShortIf",
		    FOR, LEFT_PARENTHESIS,
		    gr.zeroOrOne ("ForInit"), SEMICOLON,
		    gr.zeroOrOne ("Expression"), SEMICOLON,
		    gr.zeroOrOne ("ForUpdate"),
		    RIGHT_PARENTHESIS, "StatementNoShortIf");
	gr.addRule ("ForInit",
		    gr.oneOf ("StatementExpressionList",
			      "LocalVariableDeclaration"));
	gr.addRule ("ForUpdate", "StatementExpressionList");
	gr.addRule ("StatementExpressionList",
		    "StatementExpression", gr.zeroOrMore (COMMA, "StatementExpression"));
	gr.addRule ("EnhancedForStatement",
		    FOR, LEFT_PARENTHESIS,
		    gr.oneOf (gr.zeroOrOne ("VariableModifiers"), gr.zeroOrMore ("Annotation")),
		    "UnannType", "VariableDeclaratorId", COLON, "Expression", RIGHT_PARENTHESIS,
		    "Statement");
	gr.addRule ("EnhancedForStatementNoShortIf",
		    FOR, LEFT_PARENTHESIS,
		    gr.oneOf (gr.zeroOrOne ("VariableModifiers"), gr.zeroOrMore ("Annotation")),
		    "UnannType", "VariableDeclaratorId", COLON, "Expression", RIGHT_PARENTHESIS,
		    "StatementNoShortIf");
	gr.addRule ("BreakStatement",
		    BREAK, gr.zeroOrOne (IDENTIFIER), SEMICOLON);
	gr.addRule ("ContinueStatement",
		    CONTINUE, gr.zeroOrOne (IDENTIFIER), SEMICOLON);
	gr.addRule ("ReturnStatement",
		    RETURN, gr.zeroOrOne ("Expression"), SEMICOLON);
	gr.addRule ("ThrowStatement",
		    THROW, "Expression", SEMICOLON);
	gr.addRule ("SynchronizedStatement",
		    SYNCHRONIZED, LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS, "Block");
	gr.addRule ("TryStatement",
		    gr.oneOf (gr.sequence (TRY, "Block", "Catches"),
			      gr.sequence (TRY, "Block", gr.zeroOrOne ("Catches"), "Finally"),
			      "TryWithResourcesStatement"));
	gr.addRule ("Catches", "CatchClause", gr.zeroOrMore ("CatchClause"));
	gr.addRule ("CatchClause", CATCH, LEFT_PARENTHESIS, "CatchFormalParameter", RIGHT_PARENTHESIS, "Block");
	gr.addRule ("CatchFormalParameter",
		    gr.oneOf (gr.zeroOrOne ("VariableModifiers"), gr.zeroOrMore ("Annotation")),
		    "CatchType", "VariableDeclaratorId");
	gr.addRule ("CatchType", "UnannClassType", gr.zeroOrMore (OR, "ClassType"));
	gr.addRule ("Finally", FINALLY, "Block");
	gr.addRule ("TryWithResourcesStatement",
		    TRY, "ResourceSpecification", "Block", gr.zeroOrOne ("Catches"), gr.zeroOrOne ("Finally"));
	gr.addRule ("ResourceSpecification",
		    LEFT_PARENTHESIS, "ResourceList", gr.zeroOrOne (SEMICOLON), RIGHT_PARENTHESIS);
	gr.addRule ("ResourceList", "Resource", gr.zeroOrMore (SEMICOLON, "Resource"));
	gr.addRule ("Resource",
		    gr.oneOf (gr.zeroOrOne ("VariableModifiers"), gr.zeroOrMore ("Annotation")),
		    "UnannType", "VariableDeclaratorId", EQUAL, "Expression");
    }

    // Productions from §15 (Expressions)
    public void addExpressions () {
	gr.addRule ("Primary", gr.oneOf ("PrimaryNoNewArray", "ArrayCreationExpression"));
	gr.addRule ("PrimaryNoNewArray",
		    gr.oneOf ("Literal",
			      gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"),
					   gr.zeroOrMore (LEFT_BRACKET, RIGHT_BRACKET), DOT, CLASS),
			      gr.sequence (VOID, DOT, CLASS),
			      THIS,
			      gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"), DOT, THIS),
			      gr.sequence (LEFT_PARENTHESIS, "Expression", RIGHT_PARENTHESIS),
			      "ClassInstanceCreationExpression",
			      "FieldAccess",
			      "ArrayAccess",
			      "MethodInvocation",
			      "MethodReference"));
	gr.addRule ("ClassInstanceCreationExpression",
		    gr.zeroOrOne (gr.oneOf (IDENTIFIER, "MultiName", "Primary"), DOT),
		    NEW, gr.zeroOrOne ("TypeArguments"),
		    gr.zeroOrMore ("Annotation"), IDENTIFIER,
		    gr.zeroOrMore (DOT, gr.zeroOrMore ("Annotation"), IDENTIFIER),
		    gr.zeroOrOne ("TypeArgumentsOrDiamond"),
		    LEFT_PARENTHESIS, gr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS,
		    gr.zeroOrOne ("ClassBody"));
	gr.addRule ("TypeArgumentsOrDiamond",
		    gr.oneOf ("TypeArguments", gr.sequence (LT, GT)));
	gr.addRule ("FieldAccess",
		    gr.oneOf (gr.sequence ("Primary", DOT, IDENTIFIER),
			      gr.sequence (SUPER, DOT, IDENTIFIER),
			      gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"),
					   DOT, SUPER, DOT, IDENTIFIER)));
	gr.addRule ("ArrayAccess",
		    gr.oneOf (gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"),
					   LEFT_BRACKET, "Expression", RIGHT_BRACKET),
			      gr.sequence ("PrimaryNoNewArray", LEFT_BRACKET, "Expression", RIGHT_BRACKET)));
	gr.addRule ("MethodInvocation",
		    gr.oneOf (gr.sequence (IDENTIFIER,
					   LEFT_PARENTHESIS, gr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"),
					   DOT, gr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, gr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      gr.sequence ("Primary", DOT, gr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, gr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      gr.sequence (SUPER, DOT, gr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, gr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS),
			      gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"),
					   DOT, SUPER, DOT, gr.zeroOrOne ("TypeArguments"), IDENTIFIER,
					   LEFT_PARENTHESIS, gr.zeroOrOne ("ArgumentList"), RIGHT_PARENTHESIS)));
	gr.addRule ("ArgumentList",
		    "Expression", gr.zeroOrMore (COMMA, "Expression"));
	gr.addRule ("MethodReference",
		    gr.oneOf (gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"),
					   DOUBLE_COLON, gr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      gr.sequence ("ReferenceType", DOUBLE_COLON,
					   gr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      gr.sequence ("Primary", DOUBLE_COLON,
					   gr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      gr.sequence (SUPER, DOUBLE_COLON,
					   gr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"),
					   DOT, SUPER, DOUBLE_COLON, gr.zeroOrOne ("TypeArguments"), IDENTIFIER),
			      gr.sequence ("ClassType", DOUBLE_COLON,
					   gr.zeroOrOne ("TypeArguments"), NEW),
			      gr.sequence ("ArrayType", DOUBLE_COLON, NEW)));
	gr.addRule ("ArrayCreationExpression",
		    gr.oneOf (gr.sequence (NEW, "PrimitiveType", "DimExprs", gr.zeroOrOne ("Dims")),
			      gr.sequence (NEW, "ClassType", "DimExprs", gr.zeroOrOne ("Dims")),
			      gr.sequence (NEW, "PrimitiveType", "Dims", "ArrayInitializer"),
			      gr.sequence (NEW, "ClassType", "Dims", "ArrayInitializer")));
	gr.addRule ("DimExprs", "DimExpr", gr.zeroOrMore ("DimExpr"));
	gr.addRule ("DimExpr",
		    gr.zeroOrMore ("Annotation"), LEFT_BRACKET, "Expression", RIGHT_BRACKET);
	gr.addRule ("ConstantExpression", "Expression");
	gr.addRule ("Expression", gr.oneOf ("LambdaExpression", "AssignmentExpression"));
	gr.addRule ("LambdaExpression",
		    gr.oneOf (IDENTIFIER, "LambdaParameters"), ARROW, "LambdaBody");
	gr.addRule ("LambdaParameters",
		    LEFT_PARENTHESIS,
		    gr.oneOf (gr.zeroOrOne ("FormalParameterList"),
			      gr.sequence (IDENTIFIER, gr.zeroOrMore (COMMA, IDENTIFIER))),
		    RIGHT_PARENTHESIS);
	gr.addRule ("LambdaBody", gr.oneOf ("Expression", "Block"));
	gr.addRule ("AssignmentExpression", gr.oneOf ("ConditionalExpression", "Assignment"));
	gr.addRule ("Assignment",
		    gr.oneOf (IDENTIFIER, "MultiName", "FieldAccess", "ArrayAccess"),
		    "AssignmentOperator", "Expression");
	gr.addRule ("AssignmentOperator",
		    gr.oneOf (EQUAL,
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
	gr.addRule ("ConditionalExpression",
		    gr.oneOf ("ConditionalOrExpression",
			      gr.sequence ("ConditionalOrExpression", QUESTIONMARK,
					   "Expression", COLON, "ConditionalExpression")));
	gr.addRule ("ConditionalOrExpression",
		    gr.oneOf ("ConditionalAndExpression",
			      gr.sequence ("ConditionalOrExpression", LOGICAL_OR, "ConditionalAndExpression")));
	gr.addRule ("ConditionalAndExpression",
		    gr.oneOf ("InclusiveOrExpression",
			      gr.sequence ("ConditionalAndExpression", LOGICAL_AND, "InclusiveOrExpression")));
	gr.addRule ("InclusiveOrExpression",
		    gr.oneOf ("ExclusiveOrExpression",
			      gr.sequence ("InclusiveOrExpression", OR, "ExclusiveOrExpression")));
	gr.addRule ("ExclusiveOrExpression",
		    gr.oneOf ("AndExpression",
			      gr.sequence ("ExclusiveOrExpression", XOR, "AndExpression")));
	gr.addRule ("AndExpression",
		    gr.oneOf ("EqualityExpression",
			      gr.sequence ("AndExpression", AND, "EqualityExpression")));
	gr.addRule ("EqualityExpression",
		    gr.oneOf ("RelationalExpression",
			      gr.sequence ("EqualityExpression", DOUBLE_EQUAL, "RelationalExpression"),
			      gr.sequence ("EqualityExpression", NOT_EQUAL, "RelationalExpression")));
	gr.addRule ("RelationalExpression",
		    gr.oneOf ("ShiftExpression",
			      gr.sequence ("RelationalExpression", LT, "ShiftExpression"),
			      // This is a bit iffy, but i< is a shift/reduce conflict
			      // and this solves that problem.
			      gr.sequence (gr.oneOf (IDENTIFIER, "MultiName"), LT, "ShiftExpression"),
			      gr.sequence ("RelationalExpression", GT, "ShiftExpression"),
			      gr.sequence ("RelationalExpression", LE, "ShiftExpression"),
			      gr.sequence ("RelationalExpression", GE, "ShiftExpression"),
			      gr.sequence ("RelationalExpression", INSTANCEOF, "ReferenceType")));
	gr.addRule ("ShiftExpression",
		    gr.oneOf ("AdditiveExpression",
			      gr.sequence ("ShiftExpression", LEFT_SHIFT, "AdditiveExpression"),
			      gr.sequence ("ShiftExpression", RIGHT_SHIFT, "AdditiveExpression"),
			      gr.sequence ("ShiftExpression", RIGHT_SHIFT_UNSIGNED, "AdditiveExpression")));
	gr.addRule ("AdditiveExpression",
		    gr.oneOf ("MultiplicativeExpression",
			      gr.sequence ("AdditiveExpression", PLUS, "MultiplicativeExpression"),
			      gr.sequence ("AdditiveExpression", MINUS, "MultiplicativeExpression")));
	gr.addRule ("MultiplicativeExpression",
		    gr.oneOf ("UnaryExpression",
			      gr.sequence ("MultiplicativeExpression", MULTIPLY, "UnaryExpression"),
			      gr.sequence ("MultiplicativeExpression", DIVIDE, "UnaryExpression"),
			      gr.sequence ("MultiplicativeExpression", REMAINDER, "UnaryExpression")));
	gr.addRule ("UnaryExpression",
		    gr.oneOf ("PreIncrementExpression",
			      "PreDecrementExpression",
			      gr.sequence (PLUS, "UnaryExpression"),
			      gr.sequence (MINUS, "UnaryExpression"),
			      "UnaryExpressionNotPlusMinus"));
	gr.addRule ("PreIncrementExpression",
		    INCREMENT, "UnaryExpression");
	gr.addRule ("PreDecrementExpression",
		    DECREMENT, "UnaryExpression");
	gr.addRule ("UnaryExpressionNotPlusMinus",
		    gr.oneOf ("PostfixExpression",
			      gr.sequence (TILDE, "UnaryExpression"),
			      gr.sequence (NOT, "UnaryExpression"),
			      "CastExpression"));
	gr.addRule ("PostfixExpression",
		    gr.oneOf ("Primary",
			      gr.oneOf (IDENTIFIER, "MultiName"),
			      "PostIncrementExpression",
			      "PostDecrementExpression"));
	gr.addRule ("PostIncrementExpression",
		    "PostfixExpression", INCREMENT);
	gr.addRule ("PostDecrementExpression",
		    "PostfixExpression", DECREMENT);
	gr.addRule ("CastExpression",
		    gr.oneOf (gr.sequence (LEFT_PARENTHESIS,
					   gr.zeroOrMore ("Annotation"), gr.oneOf ("NumericType", BOOLEAN),
					   RIGHT_PARENTHESIS, "UnaryExpression"),
			      gr.sequence (LEFT_PARENTHESIS,
					   gr.oneOf (gr.sequence (gr.zeroOrMore ("Annotation"),
								  gr.oneOf (IDENTIFIER, "MultiName"),
								  gr.zeroOrOne ("TypeArguments")),
						     "NonTrivialClassType",
						     "ArrayType"),
					   gr.zeroOrMore ("AdditionalBound"),
					   RIGHT_PARENTHESIS,
					   gr.oneOf ("UnaryExpressionNotPlusMinus", "LambdaExpression"))));
    }

    public void addModifiers () {
	gr.addRule ("Modifier",
		    gr.oneOf ("Annotation",
			      PUBLIC, PROTECTED, PRIVATE,
			      ABSTRACT, STATIC, FINAL, STRICTFP, TRANSIENT,
			      VOLATILE, SYNCHRONIZED, NATIVE));
    }
}