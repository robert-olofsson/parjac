package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.util.ArrayDeque;
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

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;

import static org.khelekore.parjac.lexer.Token.*;

public class LRParser {
    private static final List<Rule> rules = new ArrayList<> ();
    private static final Map<ComplexPart, RulePart> zomRules = new HashMap<> ();
    private static final Map<String, List<Rule>> nameToRules = new HashMap<> ();
    private static final Map<String, Collection<Token>> nameToFirsts = new HashMap<> ();
    private static final StateTable table = new StateTable ();

    // TODO: currently <state id> <grammar symbol> <state id> <grammar symbol> ...
    // TODO: but can be reduced to only state ids.
    private final ArrayDeque<Object> stack = new ArrayDeque<> ();
    private final Path path;
    private final Lexer lexer;
    private final CompilerDiagnosticCollector diagnostics;

    static {
	// First rule should be the goal rule
	addRule ("Goal", "CompilationUnit", END_OF_INPUT);

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
		 zeroOrMore ("Annotation"), LEFT_BRACKET, RIGHT_BRACKET,
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
		 oneOf (IDENTIFIER,
			sequence ("PackageName", DOT, IDENTIFIER)));
	addRule ("TypeName",
		 oneOf (IDENTIFIER,
			sequence ("PackageOrTypeName", DOT, IDENTIFIER)));
	addRule ("PackageOrTypeName",
		 oneOf (IDENTIFIER,
			sequence ("PackageOrTypeName", DOT, IDENTIFIER)));
	addRule ("ExpressionName",
		 oneOf (IDENTIFIER,
			sequence ("AmbiguousName", DOT, IDENTIFIER)));
	addRule ("MethodName", IDENTIFIER);
	addRule ("AmbiguousName",
		 oneOf (IDENTIFIER,
			sequence ("AmbiguousName", DOT, IDENTIFIER)));

	/* End of §6 */

	/* Productions from §7 (Packages) */
	addRule ("CompilationUnit",
		 zeroOrOne ("PackageDeclaration"),
		 zeroOrMore ("ImportDeclaration"),
		 zeroOrMore ("TypeDeclaration"));
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
	ComplexPart[] parts = getParts (os);
	List<List<SimplePart>> simpleRules = split (parts);
	simpleRules.forEach (ls -> addRule (name, ls));
    }

    private static List<List<SimplePart>> split (ComplexPart[] parts) {
	List<List<SimplePart>> ret = new ArrayList<> ();
	ret.add (new ArrayList<> ());
	for (ComplexPart p : parts)
	    p.split (ret);
	return ret;
    }

    private static int zomCounter = 0;

    private static void addRule (String name, List<SimplePart> parts) {
	Rule r = new Rule (name, parts);
	System.out.println (r);
	rules.add (r);
	List<Rule> ls = nameToRules.get (name);
	if (ls == null) {
	    ls = new ArrayList<> ();
	    nameToRules.put (name, ls);
	}
	ls.add (r);
    }

    private static ComplexPart zeroOrOne (String rule) {
	return new ZeroOrOneRulePart (new RulePart (rule));
    }

    private static ComplexPart zeroOrOne (Token token) {
	return new ZeroOrOneRulePart (new TokenPart (token));
    }

    private static ComplexPart zeroOrMore (String rule) {
	return new ZeroOrMoreRulePart (new RulePart (rule));
    }

    private static ComplexPart zeroOrMore (Object... parts) {
	return new ZeroOrMoreRulePart (sequence (parts));
    }

    private static ComplexPart sequence (Object... os) {
	return new SequencePart (getParts (os));
    }

    private static ComplexPart oneOf (Object... os) {
	return new OneOfPart (getParts (os));
    }

    private static ComplexPart[] getParts (Object... os) {
	ComplexPart[] parts = new ComplexPart[os.length];
	for (int i = 0; i < os.length; i++) {
	    if (os[i] instanceof ComplexPart)
		parts[i] = (ComplexPart)os[i];
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
	private final List<SimplePart> parts;

	public Rule (String name, List<SimplePart> parts) {
	    this.name = name;
	    this.parts = parts;
	}

	@Override public String toString () {
	    return name + " -> " + parts;
	}

	public Collection<String> getSubrules () {
	    return parts.stream ().
		flatMap (p -> p.getSubrules ().stream ()).
		collect (Collectors.toSet ());
	}

	public boolean canBeEmpty () {
	    for (SimplePart p : parts)
		if (!p.canBeEmpty ())
		    return false;
	    return true;
	}

	public Collection<Token> getFirsts () {
	    return getFirsts (new HashSet<> ());
	}

	public Collection<Token> getFirsts (Set<String> visitedRules) {
	    visitedRules.add (name);
	    Set<Token> ret = new HashSet<> ();
	    for (SimplePart p : parts) {
		Collection<Token> firsts = p.getFirsts (visitedRules);
		ret.addAll (firsts);
		if (!p.canBeEmpty ())
		    break;
	    }
	    return ret;
	}
    }

    private interface SimplePart {
	Collection<String> getSubrules ();
	boolean canBeEmpty ();
	Collection<Token> getFirsts (Set<String> visitedRules);
    }

    private interface ComplexPart {
	void split (List<List<SimplePart>> parts);
    }

    private static class TokenPart implements SimplePart, ComplexPart {
	private final Token token;

	public TokenPart (Token token) {
	    this.token = token;
	}

	@Override public String toString () {
	    return token.toString ();
	}

	@Override public int hashCode () {
	    return token.hashCode ();
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () == getClass ())
		return token.equals (((TokenPart)o).token);
	    return false;
	}

 	@Override public Collection<String> getSubrules () {
	    return Collections.emptySet ();
	}

	@Override public boolean canBeEmpty () {
	    return false;
	}

	@Override public Collection<Token> getFirsts (Set<String> visitedRules) {
	    return Collections.singleton (token);
	}

	@Override public void split (List<List<SimplePart>> parts) {
	    parts.forEach (ls -> ls.add (this));
	}
    }

    private static class RulePart implements SimplePart, ComplexPart {
	private final String rule;
	public RulePart (String rule) {
	    this.rule = rule;
	}

	@Override public String toString () {
	    return rule;
	}

	@Override public int hashCode () {
	    return rule.hashCode ();
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () == getClass ())
		return rule.equals (((RulePart)o).rule);
	    return false;
	}

	@Override public Collection<String> getSubrules () {
	    return Collections.singleton (rule);
	}

	@Override public boolean canBeEmpty () {
	    for (Rule r : nameToRules.get (rule))
		if (!r.canBeEmpty())
		    return false;
	    return true;
	}

	@Override public Collection<Token> getFirsts (Set<String> visitedRules) {
	    if (visitedRules.contains (rule))
		return Collections.emptySet ();
	    visitedRules.add (rule);
	    Set<Token> ret = new HashSet<> ();
	    for (Rule r : nameToRules.get (rule))
		ret.addAll (r.getFirsts (visitedRules));
	    return ret;
	}

	@Override public void split (List<List<SimplePart>> parts) {
	    parts.forEach (ls -> ls.add (this));
	}
    }


    private static class ZeroOrOneRulePart implements ComplexPart {
	private final ComplexPart part;
	public ZeroOrOneRulePart (ComplexPart part) {
	    this.part = part;
	}

	@Override public String toString () {
	    return "[" + part + "]";
	}

	@Override public int hashCode () {
	    return part.hashCode ();
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () == getClass ())
		return part.equals (((ZeroOrOneRulePart)o).part);
	    return false;
	}

	@Override public void split (List<List<SimplePart>> parts) {
	    List<List<SimplePart>> newRules = new ArrayList<> ();
	    for (List<SimplePart> ls : parts)
		newRules.add (new ArrayList<> (ls));
	    part.split (newRules);
	    parts.addAll (newRules);
	}
    }

    private static class ZeroOrMoreRulePart implements ComplexPart {
	private final ComplexPart part;
	public ZeroOrMoreRulePart (ComplexPart part) {
	    this.part = part;
	}

	@Override public String toString () {
	    return "{" + part + "}";
	}

	@Override public int hashCode () {
	    return part.hashCode ();
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () == getClass ())
		return part.equals (((ZeroOrMoreRulePart)o).part);
	    return false;
	}

	@Override public void split (List<List<SimplePart>> parts) {
	    List<List<SimplePart>> newRules = new ArrayList<> ();
	    for (List<SimplePart> ls : parts)
		newRules.add (new ArrayList<> (ls));
	    RulePart newRule = findOrCreateRule ();
	    newRule.split (newRules);
	    parts.addAll (newRules);
	}

	private RulePart findOrCreateRule () {
	    RulePart rp = zomRules.get (part);
	    if (rp != null)
		return rp;
	    RulePart newRule  = new RulePart ("ZOM_" + zomCounter++);
	    zomRules.put (part, newRule);
	    addRule (newRule.rule, part);
	    addRule (newRule.rule, new RulePart (newRule.rule), part);
	    return newRule;
	}
    }

    private static class SequencePart implements ComplexPart {
	private final ComplexPart[] parts;
	public SequencePart (ComplexPart... parts) {
	    this.parts = parts;
	}

	@Override public String toString () {
	    return Arrays.asList (parts).stream ().
		map(i -> i.toString()).
		collect (Collectors.joining(" "));
	}

	@Override public int hashCode () {
	    return Arrays.hashCode (parts);
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () == getClass ())
		return Arrays.equals (parts, ((SequencePart)o).parts);
	    return false;
	}

	@Override public void split (List<List<SimplePart>> parts) {
	    for (ComplexPart spp : this.parts)
		spp.split (parts);
	}
    }

    private static class OneOfPart implements ComplexPart {
	private final ComplexPart[] parts;
	public OneOfPart (ComplexPart... parts) {
	    this.parts = parts;
	}

	@Override public String toString () {
	    return Arrays.asList (parts).stream ().
		map(i -> i.toString()).
		collect (Collectors.joining(" | "));
	}

	@Override public void split (List<List<SimplePart>> parts) {
	    parts.clear (); // TODO: currently only handled at beginning
	    for (ComplexPart oop : this.parts) {
		List<List<SimplePart>> subRules = new ArrayList<> ();
		subRules.add (new ArrayList<> ());
		oop.split (subRules);
		parts.addAll (subRules);
	    }
	}
    }

    public static void main (String[] args) {
	System.out.println ("rules has: " + rules.size () + " rules");
	validateRules ();
	memorizeFirsts ();
	Item startItem = new Item (nameToRules.get ("Goal").get (0), 0,
				   Collections.singleton (Token.END_OF_INPUT));
	System.out.println ("closure1(" + startItem + "): " + 
			    closure1 (Collections.singleton (startItem)));
    }

    private static void validateRules () {
	Set<String> validRules =
	    rules.stream ().map (r -> r.name).collect (Collectors.toSet ());

	rules.forEach (rule -> {
		for (String subrule : rule.getSubrules ())
		    if (!validRules.contains (subrule))
			System.err.println ("*" + rule + "* missing subrule: " + subrule);
	    });
	List<Rule> ls = nameToRules.get ("Goal");
	if (ls == null || ls.isEmpty())
	    System.err.println ("no Goal rule defined");
	if (ls.size () > 1)
	    System.err.println ("Multiple Goal rules defined");
    }

    private static void memorizeFirsts () {
	rules.forEach (rule -> nameToFirsts.put (rule.name, rule.getFirsts ()));
    }

    private static Set<Item> closure1 (Set<Item> s) {
	Set<Item> res = new HashSet<Item> (s);
	for (Item i : s) {
	    /* TODO:
	    Part symbolRightOfDot = r.getRulePart (i.dotPos + 1);
	    Set<Token> lookAhead = new HashSet<> ();
	    boolean empty = true;
	    for (Part p : i.r.partsAfter (i.dotPos + 1)) {
		lookAhead.addAll (p.getFirsts (new HashSet<> ()));
		if (!p.canBeEmpty ()) {
		    empty = false;
		    break;
		}
	    }
	    if (empty)
		lookAhead.addAll (i.lookAhead);
	    if (symbolRightOfDot instanceof RulePart) {
		RulePart rp = (RulePart)symbolRightOfDot;
		Rule nr = nameToRule.get (rp.rule);
		Item ni = new Item (nr, 0, lookAhead);
		res.add (ni);
	    }
	    */
	}
	return res;
    }

    private static class Item {
	private final Rule r;
	private final int dotPos;
	private final Set<Token> lookAhead;

	public Item (Rule r, int dotPos, Set<Token> lookAhead) {
	    this.r = r;
	    this.dotPos = dotPos;
	    this.lookAhead = lookAhead;
	}

	@Override public String toString () {
	    return "[" + r + ", dotPos: " + dotPos + "; " + lookAhead + "]";
	}
    }

    public LRParser (Path path, Lexer lexer, CompilerDiagnosticCollector diagnostics) {
	this.path = path;
	this.lexer = lexer;
	this.diagnostics = diagnostics;
    }

    public void parse () {
	stack.push (0);
	while (true) {
	    int currentState = (Integer)stack.peekLast ();
	    Token nextToken = lexer.nextNonWhitespaceToken ();
	    Action a = table.getAction (currentState, nextToken);
	    if (a == null) {
		addParserError ("No action for nextToken: " + nextToken);
	    } else {
		switch (a.getType ()) {
		case SHIFT:
		    stack.push (nextToken);
		    stack.push (a.getN ());
		    break;
		case REDUCE:
		    stack.pop (); // TODO: pop k things.
		    int topState = (Integer)stack.peekLast ();
		    String leftSide = "someRule";
		    Integer goTo = table.getGoTo (topState, leftSide);
		    stack.push (leftSide);
		    stack.push (goTo);
		    break;
		case ACCEPT:
		    return;
		case ERROR:
		    addParserError ("ERROR: for nextToken: " + nextToken);
		}
	    }
	}
    }

    public CompilerDiagnosticCollector getDiagnostics () {
	return diagnostics;
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
