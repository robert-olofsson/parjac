package org.khelekore.parjac.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.lexer.Token;

import static org.khelekore.parjac.lexer.Token.*;

public class LALRParser {
    private static final List<Rule> rules = new ArrayList<> ();
    private static final Map<String, Rule> nameToRule = new HashMap<> ();
    private static final String startRule = "CompilationUnit";

    static {

	/* Productions from §3 (Lexical Structure) */
	addRule ("Literal",
		 oneOf (INT_LITERAL, LONG_LITERAL,
			FLOAT_LITERAL, DOUBLE_LITERAL,
			TRUE, FALSE,
			CHARACTER_LITERAL,
			STRING_LITERAL,
			NULL));
	/* End of §3 */

	/* Productions from §4 (Types, Values, and Variables) */
	addRule ("Type",
		 oneOf ("PrimitiveType", "ReferenceType"));
	addRule ("PrimitiveType",
		 oneOf (sequence (zeroOrMore ("Annotation"), "NumericType"),
			sequence (zeroOrMore ("Annotation"), BOOLEAN)));
	addRule ("NumericType",
		 oneOf ("IntegralType", "FloatingPointType"));
	addRule ("IntegralType",
		 oneOf (BYTE, SHORT, INT, LONG, CHAR));
	addRule ("FloatingPointType",
		 oneOf (FLOAT, DOUBLE));
	addRule("ReferenceType",
		oneOf ("ClassOrInterfaceType", "TypeVariable", "ArrayType"));
	addRule ("ClassOrInterfaceType",
		 oneOf ("ClassType", "InterfaceType"));
	addRule ("ClassType",
		 oneOf (sequence (zeroOrMore ("Annotation"), IDENTIFIER, zeroOrOne ("TypeArguments")),
			sequence ("ClassOrInterfaceType", DOT, zeroOrMore ("Annotation"),
				  IDENTIFIER, zeroOrOne ("TypeArguments"))));
	addRule ("InterfaceType", "ClassType");
	addRule ("TypeVariable", zeroOrMore ("Annotation", IDENTIFIER));
	addRule ("ArrayType",
		 oneOf (sequence ("PrimitiveType", "Dims"),
			sequence ("ClassOrInterfaceType", "Dims"),
			sequence ("TypeVariable", "Dims")));
	addRule ("Dims",
		 zeroOrMore("Annotation"), LEFT_BRACKET, RIGHT_BRACKET,
		 zeroOrMore (zeroOrMore ("Annotation"),  LEFT_BRACKET, RIGHT_BRACKET));
	addRule ("TypeParameter",
		 zeroOrMore ("TypeParameterModifier"), IDENTIFIER, zeroOrOne ("TypeBound"));
	addRule ("TypeParameterModifier", "Annotation");
	addRule ("TypeBound",
		 oneOf (sequence (EXTENDS, "TypeVariable"),
			sequence (EXTENDS, "ClassOrInterfaceType", zeroOrMore ("AdditionalBound"))));
	addRule ("AdditionalBound", AND, "InterfaceType");
	addRule ("TypeArguments", LT, "TypeArgumentList", GT);
	addRule ("TypeArgumentList",
		 "TypeArgument", zeroOrMore (sequence(COMMA, "TypeArgument")));
	addRule ("TypeArgument",
		 oneOf ("ReferenceType", "Wildcard"));
	addRule ("Wildcard",
		 zeroOrMore ("Annotation"), QUESTIONMARK, zeroOrOne ("WildcardBounds"));
	addRule ("WildcardBounds",
		 oneOf (sequence (EXTENDS, "ReferenceType"),
			sequence (SUPER, "ReferenceType")));
	/* End of §4 */

	/* Productions from §6 Names*/
	addRule ("PackageName",
		 oneOf (IDENTIFIER, sequence ("PackageName", DOT, IDENTIFIER)));
	addRule ("TypeName",
		 oneOf (IDENTIFIER, sequence ("PackageOrTypeName", DOT, IDENTIFIER)));
	addRule ("PackageOrTypeName",
		 oneOf (IDENTIFIER, sequence ("PackageOrTypeName", DOT, IDENTIFIER)));
	addRule ("ExpressionName",
		 oneOf (IDENTIFIER, sequence ("AmbiguousName", DOT, IDENTIFIER)));
	addRule ("MethodName", IDENTIFIER);
	addRule ("AmbiguousName",
		 oneOf (IDENTIFIER, sequence ("AmbiguousName", DOT, IDENTIFIER)));

	/* End of §6 */

	/* Productions from §7 (Packages) */
	addRule ("CompilationUnit",
		 zeroOrOne ("PackageDeclaration"),
		 zeroOrMore ("ImportDeclaration"),
		 zeroOrMore ("TypeDeclaration"),
		 END_OF_INPUT);
	addRule ("PackageDeclaration",
		 zeroOrMore ("PackageModifier"), PACKAGE, IDENTIFIER, zeroOrMore (DOT, IDENTIFIER), SEMICOLON);
	addRule ("PackageModifier", "Annotation");
	addRule ("ImportDeclaration",
		 oneOf ("SingleTypeImportDeclaration", "TypeImportOnDemandDeclaration",
			"SingleStaticImportDeclaration", "StaticImportOnDemandDeclaration"));
	addRule ("SingleTypeImportDeclaration",
		 IMPORT, "TypeName", SEMICOLON);
	addRule ("TypeImportOnDemandDeclaration",
		 IMPORT, "PackageOrTypeName", DOT, MULTIPLY, SEMICOLON);
	addRule ("SingleStaticImportDeclaration",
		 IMPORT, STATIC, "TypeName", DOT, IDENTIFIER, SEMICOLON);
	addRule ("StaticImportOnDemandDeclaration",
		 IMPORT, STATIC, "TypeName", DOT, MULTIPLY, SEMICOLON);
	addRule ("TypeDeclaration",
		 oneOf ("ClassDeclaration", "InterfaceDeclaration", SEMICOLON));
	/* End of §7 */

	/* Productions from §8 (Classes) */
	addRule ("ClassDeclaration",
		 oneOf ("NormalClassDeclaration", "EnumDeclaration"));
	addRule ("NormalClassDeclaration",
		 zeroOrMore ("ClassModifier"), CLASS, IDENTIFIER,
		 zeroOrOne ("TypeParameters"), zeroOrOne ("Superclass"),
		 zeroOrOne ("Superinterfaces"), "ClassBody");
	addRule ("ClassModifier",
		 oneOf ("Annotation", PUBLIC, PROTECTED, PRIVATE,
			ABSTRACT, STATIC, FINAL, STRICTFP));
	addRule ("TypeParameters",
		 LT, "TypeParameterList", GT);
	addRule ("TypeParameterList",
		 "TypeParameter", zeroOrMore (COMMA, "TypeParameter"));
	addRule ("Superclass", EXTENDS, "ClassType");
	addRule ("Superinterfaces", IMPLEMENTS, "InterfaceTypeList");
	addRule ("InterfaceTypeList", "InterfaceType", zeroOrMore (COMMA, "InterfaceType"));
	addRule ("ClassBody",
		 LEFT_CURLY, /* TODO: oneOrMore ("ClassBodyDeclaration"), */RIGHT_CURLY);

	addRule ("EnumDeclaration",
		 zeroOrMore ("ClassModifier",
			     ENUM, IDENTIFIER, zeroOrOne ("Superinterfaces"), "EnumBody"));
	addRule ("EnumBody",
		 LEFT_CURLY,
		 /* zeroOrOne ("EnumConstantList"), zeroOrOne (COMMA),
		    zerOrOne ("EnumBodyDeclarations"), */
		 RIGHT_CURLY);


	/* End of §8 */

	/* Productions from §9 (Interfaces) */
	addRule ("InterfaceDeclaration",
		 oneOf ("NormalInterfaceDeclaration", "AnnotationTypeDeclaration"));
	addRule ("NormalInterfaceDeclaration",
		 zeroOrMore ("InterfaceModifier"), INTERFACE, IDENTIFIER,
		 zeroOrOne ("TypeParameters"), zeroOrOne ("ExtendsInterfaces"), "InterfaceBody");
	addRule ("InterfaceModifier",
		 oneOf ("Annotation", PUBLIC, PROTECTED, PRIVATE,
			ABSTRACT, STATIC, STRICTFP));
	addRule ("ExtendsInterfaces",
		 EXTENDS, "InterfaceTypeList");
	addRule ("InterfaceBody",
		 LEFT_CURLY, /* zeroOrMore ("InterfaceMemberDeclaration"), */RIGHT_CURLY);

	addRule ("AnnotationTypeDeclaration",
		 zeroOrMore ("InterfaceModifier"),
		 AT, INTERFACE, IDENTIFIER, "AnnotationTypeBody");
	addRule ("AnnotationTypeBody",
		 LEFT_CURLY, /* zeroOrMore ("AnnotationTypeMemberDeclaration"), */RIGHT_CURLY);

	addRule ("Annotation",
		 oneOf ("NormalAnnotation", "MarkerAnnotation", "SingleElementAnnotation"));
	addRule ("NormalAnnotation",
		 AT, "TypeName", LEFT_PARENTHESIS, zeroOrOne ("ElementValuePairList"));
	addRule ("ElementValuePairList",
		 "ElementValuePair", zeroOrMore (COMMA, "ElementValuePair"));
	addRule ("ElementValuePair",
		 IDENTIFIER, EQUAL, "ElementValue");
	addRule ("ElementValue",
		 oneOf (/*"ConditionalExpression", */"ElementValueArrayInitializer", "Annotation"));
	addRule ("ElementValueArrayInitializer",
		 LEFT_CURLY, zeroOrOne ("ElementValueList"), zeroOrOne (COMMA));
	addRule ("ElementValueList",
		 "ElementValue", zeroOrMore (COMMA, "ElementValue"));
	addRule ("MarkerAnnotation",
		 AT, "TypeName");
	addRule ("SingleElementAnnotation",
		 AT, "TypeName", LEFT_PARENTHESIS, "ElementValue", RIGHT_PARENTHESIS);

	/* End of §9 */

	/* Productions from §10 (Arrays) */
	/* End of §10 */

	/* Productions from §14 (Blocks and Statements) */
	/* End of §14 */

	/* Productions from §15 (Expressions) */
	/* End of §15 */
    }

    private static void addRule (String name, Object... os) {
	Rule r = new Rule (name, getParts (os));
	rules.add (r);
	nameToRule.put (name, r);
    }

    private static Part zeroOrOne (String rule) {
	return new ZeroOrOneRulePart (new RulePart (rule));
    }

    private static Part zeroOrOne (Token token) {
	return new ZeroOrOneRulePart (new TokenPart (token));
    }

    private static Part zeroOrMore (String rule) {
	return new ZeroOrMoreRulePart (new RulePart (rule));
    }

    private static Part zeroOrMore (Token... tokens) {
	return new ZeroOrMoreRulePart (new TokenPart (tokens));
    }

    private static Part zeroOrMore (Object... parts) {
	return new ZeroOrOneRulePart (sequence (parts));
    }

    private static Part sequence (Object... os) {
	return new SequencePart (getParts (os));
    }

    private static Part oneOf (Object... os) {
	return new OneOfPart (getParts (os));
    }

    private static Part[] getParts (Object... os) {
	Part[] parts = new Part[os.length];
	for (int i = 0; i < os.length; i++) {
	    if (os[i] instanceof Part)
		parts[i] = (Part)os[i];
	    else if (os[i] instanceof Token)
		parts[i] = new TokenPart ((Token)os[i]);
	    else if (os[i] instanceof String)
		parts[i] = new RulePart ((String)os[i]);
	    else
		throw new IllegalArgumentException ("Unknown part: " + os[i]);
	}
	return parts;
    }

    private static class Rule {
	private final String name;
	private final SequencePart seq;

	public Rule (String name, Part... parts) {
	    this.name = name;
	    this.seq = new SequencePart (parts);
	}

	@Override public String toString () {
	    return name + " -> " + seq;
	}

	public Collection<String> getSubrules () {
	    return seq.getSubrules ();
	}

	public boolean canBeEmpty () {
	    return seq.canBeEmpty ();
	}

	public Collection<Token> getFirsts () {
	    return getFirsts (new HashSet<> ());
	}

	public Collection<Token> getFirsts (Set<String> visitedRules) {
	    visitedRules.add (name);
	    return seq.getFirsts (visitedRules);
	}
    }

    private interface Part {
	Collection<String> getSubrules ();
	boolean canBeEmpty ();
	Collection<Token> getFirsts (Set<String> visitedRules);
    }

    private static class TokenPart implements Part {
	private final Token[] tokens;
	public TokenPart (Token... tokens) {
	    this.tokens = tokens;
	}

 	public Collection<String> getSubrules () {
	    return Collections.emptySet ();
	}

	public boolean canBeEmpty () {
	    return tokens.length == 0;
	}

	@Override public String toString () {
	    return Arrays.asList (tokens).stream ().
		map(i -> i.toString()).
		collect (Collectors.joining(" "));
	}

	@Override public Collection<Token> getFirsts (Set<String> visitedRules) {
	    if (tokens.length == 0)
		return Collections.emptySet ();
	    return Collections.singleton (tokens[0]);
	}
    }

    private static class ZeroOrOneRulePart implements Part {
	private final Part part;
	public ZeroOrOneRulePart (Part part) {
	    this.part = part;
	}

 	public Collection<String> getSubrules () {
	    return part.getSubrules ();
	}

	public boolean canBeEmpty () {
	    return true;
	}

	@Override public Collection<Token> getFirsts (Set<String> visitedRules) {
	    return part.getFirsts (visitedRules);
	}

	@Override public String toString () {
	    return "[" + part + "]";
	}
    }

    private static class ZeroOrMoreRulePart implements Part {
	private final Part part;
	public ZeroOrMoreRulePart (Part part) {
	    this.part = part;
	}

	public Collection<String> getSubrules () {
	    return part.getSubrules ();
	}

	public boolean canBeEmpty () {
	    return true;
	}

	@Override public Collection<Token> getFirsts (Set<String> visitedRules) {
	    return part.getFirsts (visitedRules);
	}

	@Override public String toString () {
	    return "{" + part + "}";
	}
    }

    private static class SequencePart implements Part {
	private final Part[] parts;
	public SequencePart (Part... parts) {
	    this.parts = parts;
	}

	public Collection<String> getSubrules () {
	    return Arrays.asList (parts).stream ().
		flatMap (p -> p.getSubrules ().stream ()).
		collect (Collectors.toSet ());
	}

	public boolean canBeEmpty () {
	    for (Part p : parts)
		if (!p.canBeEmpty ())
		    return false;
	    return true;
	}

	@Override public Collection<Token> getFirsts (Set<String> visitedRules) {
	    Set<Token> ret = new HashSet<> ();
	    for (Part p : parts) {
		Collection<Token> firsts = p.getFirsts (visitedRules);
		ret.addAll (firsts);
		if (!p.canBeEmpty ())
		    break;
	    }
	    return ret;
	}

	@Override public String toString () {
	    return Arrays.asList (parts).stream ().
		map(i -> i.toString()).
		collect (Collectors.joining(" "));
	}
    }

    private static class OneOfPart implements Part {
	private final Part[] parts;
	public OneOfPart (Part... parts) {
	    this.parts = parts;
	}

	public Collection<String> getSubrules () {
	    return Arrays.asList (parts).stream ().
		flatMap (p -> p.getSubrules ().stream ()).
		collect (Collectors.toSet ());
	}

	public boolean canBeEmpty () {
	    for (Part p : parts)
		if (!p.canBeEmpty ())
		    return false;
	    return true;
	}

	@Override public Collection<Token> getFirsts (Set<String> visitedRules) {
	    Set<Token> ret = new HashSet<> ();
	    for (Part p : parts)
		ret.addAll (p.getFirsts (visitedRules));
	    return ret;
	}

	@Override public String toString () {
	    return Arrays.asList (parts).stream ().
		map(i -> i.toString()).
		collect (Collectors.joining(" | "));
	}
    }

    private static class RulePart implements Part {
	private final String rule;
	public RulePart (String rule) {
	    this.rule = rule;
	}

	public Collection<String> getSubrules () {
	    return Collections.singleton (rule);
	}

	public boolean canBeEmpty () {
	    return nameToRule.get (rule).canBeEmpty ();
	}

	@Override public Collection<Token> getFirsts (Set<String> visitedRules) {
	    if (visitedRules.contains (rule))
		return Collections.emptySet ();
	    visitedRules.add (rule);
	    return nameToRule.get (rule).getFirsts (visitedRules);
	}

	@Override public String toString () {
	    return rule;
	}
    }

    public static void main (String[] args) {
	validateRules ();
    }

    private static void validateRules () {
	System.out.println ("rules has: " + rules.size () + " rules");
	Set<String> validRules =
	    rules.stream ().map (r -> r.name).collect (Collectors.toSet ());

	rules.forEach (rule -> {
		for (String subrule : rule.getSubrules ())
		    if (!validRules.contains (subrule))
			System.err.println ("*" + rule + "* missing subrule: " + subrule);
	    });

	rules.forEach (rule ->
		       System.err.println (rule.name + " has first: " + rule.getFirsts ()));
    }
}
