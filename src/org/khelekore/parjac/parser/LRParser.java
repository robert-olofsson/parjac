package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;

import static org.khelekore.parjac.lexer.Token.*;

public class LRParser {
    // All the generated rules
    private static final List<Rule> rules = new ArrayList<> ();
    private static final List<RuleCollection> ruleCollections = new ArrayList<> ();
    // Index from name to all the generated rules
    private static final Map<String, RuleCollection> nameToRules = new HashMap<> ();

    // All the zero or more rules, so that we can merge such rules
    private static final Map<ComplexPart, RulePart> zomRules = new HashMap<> ();

    private static final StateTable table = new StateTable ();

    // TODO: currently <state id> <grammar symbol> <state id> <grammar symbol> ...
    // TODO: but can be reduced to only state ids.
    private final ArrayDeque<Object> stack = new ArrayDeque<> ();

    // The file we are parsing
    private final Path path;

    // The lexer we are using to get tokens
    private final Lexer lexer;

    // Compiler output
    private final CompilerDiagnosticCollector diagnostics;

    static {
	/* test grammars
	addRule ("Goal", "S", END_OF_INPUT);
	addRule ("S",
		 oneOf (sequence (AT, "S", LEFT_CURLY),
			"B"));
	addRule ("B",
		 oneOf (sequence (LEFT_BRACKET, "B", LEFT_CURLY),
			"C"));
	addRule ("C",
		 oneOf (sequence (LEFT_PARENTHESIS, "C", LEFT_CURLY),
			RIGHT_BRACKET));
	addRule ("S", "A", "B", LEFT_PARENTHESIS);
	addRule ("A", zeroOrOne (AT));
	addRule ("B", zeroOrOne (LEFT_BRACKET));
	*/
	// First rule should be the goal rule
	addRule ("Goal", "CompilationUnit", END_OF_INPUT);

	// Productions from §3 (Lexical Structure)
	addRule ("Literal",
		 oneOf (INT_LITERAL, LONG_LITERAL,
			FLOAT_LITERAL, DOUBLE_LITERAL,
			TRUE, FALSE,
			CHARACTER_LITERAL,
			STRING_LITERAL,
			NULL));
	// End of §3

	// Productions from §4 (Types, Values, and Variables)
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
	addRule ("TypeVariable", zeroOrMore ("Annotation"), IDENTIFIER);
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
	// End of §4

	// Productions from §6 Names
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

	// End of §6

	// Productions from §7 (Packages)
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
	// End of §7

	// Productions from §8 (Classes)
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
		 LEFT_CURLY,
		 // TODO: oneOrMore ("ClassBodyDeclaration"),
		 RIGHT_CURLY);

	addRule ("EnumDeclaration",
		 zeroOrMore ("ClassModifier"),
		 ENUM, IDENTIFIER, zeroOrOne ("Superinterfaces"), "EnumBody");
	addRule ("EnumBody",
		 LEFT_CURLY,
		 // zeroOrOne ("EnumConstantList"), zeroOrOne (COMMA),
		 // zerOrOne ("EnumBodyDeclarations"),
		 RIGHT_CURLY);
	// End of §8

	// Productions from §9 (Interfaces)
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
		 LEFT_CURLY,
		 // zeroOrMore ("InterfaceMemberDeclaration"),
		 RIGHT_CURLY);

	addRule ("AnnotationTypeDeclaration",
		 zeroOrMore ("InterfaceModifier"),
		 AT, INTERFACE, IDENTIFIER, "AnnotationTypeBody");
	addRule ("AnnotationTypeBody",
		 LEFT_CURLY,
		 // zeroOrMore ("AnnotationTypeMemberDeclaration"),
		 RIGHT_CURLY);

	addRule ("Annotation",
		 oneOf ("NormalAnnotation", "MarkerAnnotation", "SingleElementAnnotation"));
	addRule ("NormalAnnotation",
		 AT, "TypeName", LEFT_PARENTHESIS, zeroOrOne ("ElementValuePairList"));
	addRule ("ElementValuePairList",
		 "ElementValuePair", zeroOrMore (COMMA, "ElementValuePair"));
	addRule ("ElementValuePair",
		 IDENTIFIER, EQUAL, "ElementValue");
	addRule ("ElementValue",
		 oneOf (
		 //"ConditionalExpression",
		 "ElementValueArrayInitializer",
		 "Annotation"));
	addRule ("ElementValueArrayInitializer",
		 LEFT_CURLY, zeroOrOne ("ElementValueList"), zeroOrOne (COMMA));
	addRule ("ElementValueList",
		 "ElementValue", zeroOrMore (COMMA, "ElementValue"));
	addRule ("MarkerAnnotation",
		 AT, "TypeName");
	addRule ("SingleElementAnnotation",
		 AT, "TypeName", LEFT_PARENTHESIS, "ElementValue", RIGHT_PARENTHESIS);

	// End of §9

	// Productions from §10 (Arrays)
	// End of §10

	// Productions from §14 (Blocks and Statements)
	// End of §14

	// Productions from §15 (Expressions)
	// End of §15
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
	RuleCollection rc = nameToRules.get (name);
	if (rc == null) {
	    rc = new RuleCollection (name);
	    ruleCollections.add (rc);
	    nameToRules.put (name, rc);
	}
	rc.rules.add (r);
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

    private static class RuleCollection {
	private final String name;
	private final List<Rule> rules;
	private boolean canBeEmpty = true;
	private EnumSet<Token> firsts;
	private EnumSet<Token> follow;

	public RuleCollection (String name) {
	    this.name = name;
	    rules = new ArrayList<> ();
	}
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

	public SimplePart getRulePart (int pos) {
	    return parts.get (pos);
	}

	public List<SimplePart> getPartsAfter (int pos) {
	    return parts.subList (pos, parts.size ());
	}
    }

    private interface SimplePart {
	Collection<String> getSubrules ();
	boolean canBeEmpty ();
	EnumSet<Token> getFirsts ();
    }

    private interface ComplexPart {
	void split (List<List<SimplePart>> parts);
    }

    private static abstract class PartBase<T> implements SimplePart, ComplexPart {
	protected final T data;

	public PartBase (T data) {
	    this.data = data;
	}

	@Override public String toString () {
	    return data.toString ();
	}

	@Override public int hashCode () {
	    return data.hashCode ();
	}

	@SuppressWarnings ("unchecked") @Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () == getClass ())
		return data.equals (((PartBase<T>)o).data);
	    return false;
	}

	@Override public void split (List<List<SimplePart>> parts) {
	    parts.forEach (ls -> ls.add (this));
	}
    }

    private static class TokenPart extends PartBase<Token> {

	public TokenPart (Token token) {
	    super (token);
	}

 	@Override public Collection<String> getSubrules () {
	    return Collections.emptySet ();
	}

	@Override public boolean canBeEmpty () {
	    return false;
	}

	@Override public EnumSet<Token> getFirsts () {
	    return EnumSet.of (data);
	}
    }

    private static class RulePart extends PartBase<String> {
	public RulePart (String rule) {
	    super (rule);
	}

	@Override public Collection<String> getSubrules () {
	    return Collections.singleton (data);
	}

	@Override public boolean canBeEmpty () {
	    return nameToRules.get (data).canBeEmpty;
	}

	@Override public EnumSet<Token> getFirsts () {
	    return nameToRules.get (data).firsts;
	}
    }

    private static abstract class ComplexBase implements ComplexPart {
	protected final ComplexPart part;

	public ComplexBase (ComplexPart part) {
	    this.part = part;
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
		return part.equals (((ComplexBase)o).part);
	    return false;
	}

	@Override public void split (List<List<SimplePart>> parts) {
	    List<List<SimplePart>> newRules = new ArrayList<> ();
	    for (List<SimplePart> ls : parts)
		newRules.add (new ArrayList<> (ls));
	    ComplexPart newRule = getNewRule ();
	    newRule.split (newRules);
	    parts.addAll (newRules);
	}

	public abstract ComplexPart getNewRule ();
    }

    private static class ZeroOrOneRulePart extends ComplexBase {
	public ZeroOrOneRulePart (ComplexPart part) {
	    super (part);
	}

	@Override public String toString () {
	    return "[" + part + "]";
	}

	public ComplexPart getNewRule () {
	    return part;
	}
    }

    private static class ZeroOrMoreRulePart extends ComplexBase {
	public ZeroOrMoreRulePart (ComplexPart part) {
	    super (part);
	}

	@Override public String toString () {
	    return "{" + part + "}";
	}

	public ComplexPart getNewRule () {
	    RulePart rp = zomRules.get (part);
	    if (rp != null)
		return rp;
	    RulePart newRule  = new RulePart ("ZOM_" + zomCounter++);
	    zomRules.put (part, newRule);
	    addRule (newRule.data, part);
	    addRule (newRule.data, new RulePart (newRule.data), part);
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
	memorizeEmpty ();
	memorizeFirsts ();
	memorizeFollows ();
	/*
	Item startItem = new Item (nameToRules.get ("Goal").get (0), 0,
				   Collections.singleton (END_OF_INPUT));
	System.out.println ("closure1(" + startItem + "): " +
			    closure1 (Collections.singleton (startItem)));
	*/
    }

    private static void validateRules () {
	Set<String> validRules =
	    rules.stream ().map (r -> r.name).collect (Collectors.toSet ());

	rules.forEach (rule -> {
		for (String subrule : rule.getSubrules ())
		    if (!validRules.contains (subrule))
			System.err.println ("*" + rule + "* missing subrule: " + subrule);
	    });
	List<Rule> ls = nameToRules.get ("Goal").rules;
	if (ls == null || ls.isEmpty())
	    System.err.println ("no Goal rule defined");
	if (ls.size () > 1)
	    System.err.println ("Multiple Goal rules defined");
    }

    private static void memorizeEmpty () {
	ruleCollections.forEach (rc -> rc.canBeEmpty = false);
	boolean thereWasChanges;
	do {
	    thereWasChanges = false;
	    for (RuleCollection rc : ruleCollections) {
		if (!rc.canBeEmpty) {
		    if (canBeEmpty (rc)) {
			rc.canBeEmpty = true;
			thereWasChanges = true;
		    }
		}
	    }
	} while (thereWasChanges);
    }

    private static boolean canBeEmpty (RuleCollection rc) {
	boolean ret = false;
	for (Rule r : rules) {
	    if (r.parts.isEmpty ())
		return true;
	    if (r.parts.stream ().allMatch (sp -> sp.canBeEmpty ()))
		return true;
	}
	return false;
    }

    private static void memorizeFirsts () {
	ruleCollections.forEach (rc -> rc.firsts = EnumSet.noneOf (Token.class));
	boolean thereWasChanges;
	do {
	    thereWasChanges = false;
	    for (RuleCollection rc : ruleCollections)
		thereWasChanges |= rc.firsts.addAll (getFirsts (rc));
	} while (thereWasChanges);
	ruleCollections.forEach (rc -> System.out.println (rc.name + " has firsts: " + rc.firsts));
    }

    private static EnumSet<Token> getFirsts (RuleCollection rc) {
	EnumSet<Token> firsts = EnumSet.noneOf (Token.class);
	if (rc.rules.isEmpty ())
	    return firsts;
	for (Rule r : rc.rules) {
	    for (SimplePart sp : r.parts) {
		firsts.addAll (sp.getFirsts ());
		if (!sp.canBeEmpty ())
		    break;
	    }
	}
	return firsts;
    }

    private static void memorizeFollows () {
	ruleCollections.forEach (rc -> rc.follow = EnumSet.noneOf (Token.class));
	boolean thereWasChanges;
	do {
	    thereWasChanges = false;
	    for (Rule r : rules) {
		int s = r.parts.size ();
		for (int i = 0; i < s; i++) {
		    SimplePart sp1 = r.parts.get (i);
		    if (sp1 instanceof TokenPart)
			continue;
		    RulePart rp1 = (RulePart)sp1;
		    boolean restCanBeEmpty = true;
		    for (int j = i + 1; j < s; j++) {
			SimplePart sp2 = r.parts.get (j);
			thereWasChanges |= addFollow (rp1.data, sp2);
			if (!sp2.canBeEmpty ()) {
			    restCanBeEmpty = false;
			    break;
			}
		    }
		    if (restCanBeEmpty)
			thereWasChanges |= addFollow (rp1.data, nameToRules.get (r.name).follow);
		}
	    }
	} while (thereWasChanges);
	ruleCollections.forEach (r -> System.out.println (r.name + " has follow: " + r.follow));
    }

    private static boolean addFollow (String rule, SimplePart sp) {
	if (sp instanceof TokenPart) {
	    EnumSet<Token> ts = EnumSet.of (((TokenPart)sp).data);
	    return addFollow (rule, ts);
	}
	RulePart rp = (RulePart)sp;
	return addFollow (rule, nameToRules.get (rp.data).firsts);
    }

    private static boolean addFollow (String rule, EnumSet<Token> ts) {
	return nameToRules.get (rule).follow.addAll (ts);
    }

    private static Set<Item> closure1 (Set<Item> s) {
	boolean thereWasChanges;
	Set<Item> res;
	do {
	    res = new HashSet<Item> (s);
	    thereWasChanges = false;
	    for (Item i : s) {
		if (i.r.parts.size () <= i.dotPos)
		    continue;
		SimplePart symbolRightOfDot = i.r.getRulePart (i.dotPos);
		EnumSet<Token> lookAhead = EnumSet.noneOf (Token.class);
		boolean empty = true;
		for (SimplePart p : i.r.getPartsAfter (i.dotPos + 1)) {
		    addLookahead (lookAhead, p);
		    if (!p.canBeEmpty ()) {
			empty = false;
			break;
		    }
		}
		if (empty)
		    lookAhead.addAll (i.lookAhead);
		if (symbolRightOfDot instanceof RulePart) {
		    RulePart rp = (RulePart)symbolRightOfDot;
		    List<Rule> nrs = nameToRules.get (rp.data).rules;
		    for (Rule nr : nrs) {
			Item ni = new Item (nr, 0, lookAhead);
			if (!res.contains (ni)) {
			    res.add (ni);
			    thereWasChanges = true;
			}
		    }
		}
	    }
	    s = res;
	} while (thereWasChanges);
	return res;
    }

    private static void addLookahead (EnumSet<Token> lookAhead, SimplePart sp) {
	if (sp instanceof TokenPart) {
	    TokenPart tp = (TokenPart)sp;
	    lookAhead.add (tp.data);
	} else {
	    RulePart rp = (RulePart)sp;
	    lookAhead.addAll (nameToRules.get (rp.data).firsts);
	}
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

	@Override public int hashCode () {
	    return r.hashCode () + dotPos + lookAhead.hashCode ();
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () != getClass ())
		return false;
	    Item i = (Item)o;
	    return dotPos == i.dotPos && r.equals (i.r) && lookAhead.equals (i.lookAhead);
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
