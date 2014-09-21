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

    public static void main (String[] args) {
	Java8Grammar g = new Java8Grammar ();
    }

    private void addRules () {
	// First rule should be the goal rule
	lr.addRule ("Goal", "CompilationUnit", END_OF_INPUT);

	// Productions from §3 (Lexical Structure)
	lr.addRule ("Literal",
		    lr.oneOf (INT_LITERAL, LONG_LITERAL,
			      FLOAT_LITERAL, DOUBLE_LITERAL,
			      TRUE, FALSE,
			      CHARACTER_LITERAL,
			      STRING_LITERAL,
			      NULL));
	// End of §3

	// Productions from §4 (Types, Values, and Variables)
	lr.addRule ("Type",
		    lr.oneOf ("PrimitiveType", "ReferenceType"));
	lr.addRule ("PrimitiveType",
		    lr.oneOf (lr.sequence (lr.zeroOrMore ("Annotation"), "NumericType"),
			      lr.sequence (lr.zeroOrMore ("Annotation"), BOOLEAN)));
	lr.addRule ("NumericType",
		    lr.oneOf ("IntegralType", "FloatingPointType"));
	lr.addRule ("IntegralType",
		    lr.oneOf (BYTE, SHORT, INT, LONG, CHAR));
	lr.addRule ("FloatingPointType",
		    lr.oneOf (FLOAT, DOUBLE));
	lr.addRule("ReferenceType",
		   lr.oneOf ("ClassOrInterfaceType", "TypeVariable", "ArrayType"));
	lr.addRule ("ClassOrInterfaceType",
		    lr.oneOf ("ClassType", "InterfaceType"));
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
			      lr.sequence (EXTENDS, "ClassOrInterfaceType",
					   lr.zeroOrMore ("AdditionalBound"))));
	lr.addRule ("AdditionalBound", AND, "InterfaceType");
	lr.addRule ("TypeArguments", LT, "TypeArgumentList", GT);
	lr.addRule ("TypeArgumentList",
		    "TypeArgument", lr.zeroOrMore (lr.sequence(COMMA, "TypeArgument")));
	lr.addRule ("TypeArgument",
		    lr.oneOf ("ReferenceType", "Wildcard"));
	lr.addRule ("Wildcard",
		    lr.zeroOrMore ("Annotation"), QUESTIONMARK, lr.zeroOrOne ("WildcardBounds"));
	lr.addRule ("WildcardBounds",
		    lr.oneOf (lr.sequence (EXTENDS, "ReferenceType"),
			      lr.sequence (SUPER, "ReferenceType")));
	// End of §4

	// Productions from §6 Names
	lr.addRule ("PackageName",
		    lr.oneOf (IDENTIFIER,
			      lr.sequence ("PackageName", DOT, IDENTIFIER)));
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
		    lr.oneOf ("SingleTypeImportDeclaration", "TypeImportOnDemandDeclaration",
			      "SingleStaticImportDeclaration", "StaticImportOnDemandDeclaration"));
	lr.addRule ("SingleTypeImportDeclaration",
		    IMPORT, "TypeName", SEMICOLON);
	lr.addRule ("TypeImportOnDemandDeclaration",
		    IMPORT, "PackageOrTypeName", DOT, MULTIPLY, SEMICOLON);
	lr.addRule ("SingleStaticImportDeclaration",
		    IMPORT, STATIC, "TypeName", DOT, IDENTIFIER, SEMICOLON);
	lr.addRule ("StaticImportOnDemandDeclaration",
		    IMPORT, STATIC, "TypeName", DOT, MULTIPLY, SEMICOLON);
	lr.addRule ("TypeDeclaration",
		    lr.oneOf ("ClassDeclaration", "InterfaceDeclaration", SEMICOLON));
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
		    // TODO: oneOrMore ("ClassBodyDeclaration"),
		    RIGHT_CURLY);

	lr.addRule ("EnumDeclaration",
		    lr.zeroOrMore ("ClassModifier"),
		    ENUM, IDENTIFIER, lr.zeroOrOne ("Superinterfaces"), "EnumBody");
	lr.addRule ("EnumBody",
		    LEFT_CURLY,
		    // lrParser.zeroOrOne ("EnumConstantList"), lrParser.zeroOrOne (COMMA),
		    // zerOrOne ("EnumBodyDeclarations"),
		    RIGHT_CURLY);
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
		    // lrParser.zeroOrMore ("InterfaceMemberDeclaration"),
		    RIGHT_CURLY);

	lr.addRule ("AnnotationTypeDeclaration",
		    lr.zeroOrMore ("InterfaceModifier"),
		    AT, INTERFACE, IDENTIFIER, "AnnotationTypeBody");
	lr.addRule ("AnnotationTypeBody",
		    LEFT_CURLY,
		    // lrParser.zeroOrMore ("AnnotationTypeMemberDeclaration"),
		    RIGHT_CURLY);

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
			//"ConditionalExpression",
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
	// End of §10

	// Productions from §14 (Blocks and Statements)
	// End of §14

	// Productions from §15 (Expressions)
	// End of §15
    }
}