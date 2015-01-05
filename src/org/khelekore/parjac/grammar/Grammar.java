package org.khelekore.parjac.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.lexer.Token;

public class Grammar {
    // All the generated rules
    private final List<Rule> rules = new ArrayList<> ();
    // To avoid dups
    private final Set<Rule> ruleSet = new HashSet<> ();
    private final List<RuleCollection> ruleCollections = new ArrayList<> ();
    // Index from name to all the generated rules
    private final Map<String, RuleCollection> nameToRules = new HashMap<> ();

    // All the zero or more rules, so that we can merge such rules
    private final Map<ComplexPart, RulePart> zomRules = new HashMap<> ();
    private int zomCounter = 0; // used when generating zomRules

    public Grammar () {
	// empty
    }

    public Grammar (Grammar toCopy) {
	this.rules.addAll (toCopy.rules);
	ruleSet.addAll (toCopy.ruleSet);
	ruleCollections.addAll (toCopy.ruleCollections);
	nameToRules.putAll (toCopy.nameToRules);
	zomRules.putAll (toCopy.zomRules);
	zomCounter = toCopy.zomCounter;
    }

    public void validateRules () {
	Set<String> validRules =
	    rules.stream ().map (r -> r.getName ()).collect (Collectors.toSet ());

	rules.forEach (rule -> {
		for (String subrule : rule.getSubrules ())
		    if (!validRules.contains (subrule))
			throw new IllegalStateException ("*" + rule + "* missing subrule: " + subrule);
	    });
	for (Rule r : rules)
	    if (r.getParts ().isEmpty ())
		System.err.println ("Warning: Found empty rule: " + r);
	RuleCollection goal = nameToRules.get ("Goal");
	if (goal == null)
	    throw new IllegalStateException ("No Goal rule defined");
	List<Rule> ls = goal.getRules ();
	if (ls == null || ls.isEmpty())
	    throw new IllegalStateException ("no Goal rule defined");
	if (ls.size () > 1)
	    throw new IllegalStateException ("Multiple Goal rules defined");
    }

    public void addRule (String name, Object... os) {
	ComplexPart[] parts = getParts (os);
	List<List<SimplePart>> simpleRules = split (parts);
	simpleRules.forEach (ls -> addRule (name, ls));
    }

    public List<Rule> getRules () {
	return rules;
    }

    public int getNumberOfRules () {
	return rules.size ();
    }

    public int getNumberOfRuleNames () {
	return ruleCollections.size ();
    }

    public RuleCollection getRules (String name) {
	return nameToRules.get (name);
    }

    public void memorize () {
	memorizeEmpty ();
	memorizeFirsts ();
	memorizeFollows ();
    }

    private void memorizeEmpty () {
	ruleCollections.forEach (rc -> rc.setCanBeEmpty (false));
	boolean thereWasChanges;
	do {
	    thereWasChanges = false;
	    for (RuleCollection rc : ruleCollections) {
		if (!rc.canBeEmpty ()) {
		    if (canBeEmpty (rc)) {
			rc.setCanBeEmpty (true);
			thereWasChanges = true;
		    }
		}
	    }
	} while (thereWasChanges);
    }

    private boolean canBeEmpty (RuleCollection rc) {
	for (Rule r : getRules ()) {
	    if (r.getParts ().isEmpty ())
		return true;
	    if (r.getParts ().stream ().allMatch (sp -> sp.canBeEmpty ()))
		return true;
	}
	return false;
    }

    private void memorizeFirsts () {
	ruleCollections.forEach (rc -> rc.setFirsts (EnumSet.noneOf (Token.class)));
	boolean thereWasChanges;
	do {
	    thereWasChanges = false;
	    for (RuleCollection rc : ruleCollections)
		thereWasChanges |= rc.getFirsts ().addAll (getFirsts (rc));
	} while (thereWasChanges);
    }

    private static EnumSet<Token> getFirsts (RuleCollection rc) {
	EnumSet<Token> firsts = EnumSet.noneOf (Token.class);
	if (rc.getRules ().isEmpty ())
	    return firsts;
	for (Rule r : rc.getRules ()) {
	    for (SimplePart sp : r.getParts ()) {
		firsts.addAll (sp.getFirsts ());
		if (!sp.canBeEmpty ())
		    break;
	    }
	}
	return firsts;
    }

    private void memorizeFollows () {
	ruleCollections.forEach (rc -> rc.setFollows (EnumSet.noneOf (Token.class)));
	boolean thereWasChanges;
	do {
	    thereWasChanges = false;
	    for (Rule r : rules) {
		int s = r.getParts ().size ();
		for (int i = 0; i < s; i++) {
		    SimplePart sp1 = r.getParts ().get (i);
		    if (sp1 instanceof TokenPart)
			continue;
		    RulePart rp1 = (RulePart)sp1;
		    boolean restCanBeEmpty = true;
		    for (int j = i + 1; j < s; j++) {
			SimplePart sp2 = r.getParts ().get (j);
			thereWasChanges |= addFollow (rp1.getId (), sp2);
			if (!sp2.canBeEmpty ()) {
			    restCanBeEmpty = false;
			    break;
			}
		    }
		    if (restCanBeEmpty)
			thereWasChanges |= addFollow (rp1.getId (),
						      nameToRules.get (r.getName ()).getFollows ());
		}
	    }
	} while (thereWasChanges);
    }

    private boolean addFollow (String rule, SimplePart sp) {
	if (sp instanceof TokenPart) {
	    EnumSet<Token> ts = EnumSet.of (((TokenPart)sp).getId ());
	    return addFollow (rule, ts);
	}
	RulePart rp = (RulePart)sp;
	return addFollow (rule, nameToRules.get (rp.getId ()).getFirsts ());
    }

    private boolean addFollow (String rule, EnumSet<Token> ts) {
	return nameToRules.get (rule).getFollows ().addAll (ts);
    }

    private static List<List<SimplePart>> split (ComplexPart[] parts) {
	List<List<SimplePart>> ret = new ArrayList<> ();
	ret.add (new ArrayList<> ());
	for (ComplexPart p : parts)
	    p.split (ret);
	return ret;
    }

    private void addRule (String name, List<SimplePart> parts) {
	// Remove duplicate entries (ZOM_x ZOM_x).
	Iterator<SimplePart> i = parts.iterator ();
	SimplePart p = null;
	while (i.hasNext ()) {
	    SimplePart c = i.next ();
	    if (c.equals (p) && c.getId ().toString ().startsWith ("ZOM_"))
		i.remove ();
	    else
		p = c;
	}
	Rule r = new Rule (name, rules.size (), parts);
	// Do not add duplicate rules, they cause conflicts
	if (ruleSet.contains (r))
	    return;
	rules.add (r);
	ruleSet.add (r);
	RuleCollection rc = nameToRules.get (name);
	if (rc == null) {
	    rc = new RuleCollection ();
	    ruleCollections.add (rc);
	    nameToRules.put (name, rc);
	}
	rc.getRules ().add (r);
    }

    public ComplexPart zeroOrOne (String rule) {
	return new ZeroOrOneRulePart (new RulePart (rule, this));
    }

    public ComplexPart zeroOrOne (Token token) {
	return new ZeroOrOneRulePart (new TokenPart (token));
    }

    public ComplexPart zeroOrOne (Object... parts) {
	return new ZeroOrOneRulePart (sequence (parts));
    }

    public ComplexPart zeroOrMore (String rule) {
	return new ZeroOrMoreRulePart (new RulePart (rule, this));
    }

    public ComplexPart zeroOrMore (Object... parts) {
	return new ZeroOrMoreRulePart (sequence (parts));
    }

    public ComplexPart sequence (Object... os) {
	return new SequencePart (getParts (os));
    }

    public ComplexPart oneOf (Object... os) {
	return new OneOfPart (getParts (os));
    }

    private ComplexPart[] getParts (Object... os) {
	ComplexPart[] parts = new ComplexPart[os.length];
	for (int i = 0; i < os.length; i++) {
	    if (os[i] instanceof ComplexPart)
		parts[i] = (ComplexPart)os[i];
	    else if (os[i] instanceof Token)
		parts[i] = new TokenPart ((Token)os[i]);
	    else if (os[i] instanceof String)
		parts[i] = new RulePart ((String)os[i], this);
	    else
		throw new IllegalArgumentException ("Unknown part: " + os[i] +
						    ", os: " + Arrays.toString (os));
	}
	return parts;
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

    private class ZeroOrMoreRulePart extends ComplexBase {
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
	    RulePart newRule  = new RulePart ("ZOM_" + zomCounter++, Grammar.this);
	    zomRules.put (part, newRule);
	    addRule (newRule.data, part);
	    addRule (newRule.data, new RulePart (newRule.data, Grammar.this), part);
	    return newRule;
	}
    }

    private class SequencePart implements ComplexPart {
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
	    List<List<SimplePart>> fullSet = new ArrayList<> ();
	    for (List<SimplePart> ls : parts) {
		for (ComplexPart cp : this.parts) {
		    List<List<SimplePart>> subRules = new ArrayList<> ();
		    subRules.add (new ArrayList<> (ls));
		    cp.split (subRules);
		    fullSet.addAll (subRules);
		}
	    }
	    parts.clear ();
	    parts.addAll (fullSet);
	}
    }
}