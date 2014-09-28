package org.khelekore.parjac.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.lexer.Token;

import static org.khelekore.parjac.lexer.Token.*;

public class LRParser {
    // All the generated rules
    private final List<Rule> rules = new ArrayList<> ();
    private final List<RuleCollection> ruleCollections = new ArrayList<> ();
    // Index from name to all the generated rules
    private final Map<String, RuleCollection> nameToRules = new HashMap<> ();

    // All the zero or more rules, so that we can merge such rules
    private final Map<ComplexPart, RulePart> zomRules = new HashMap<> ();
    private int zomCounter = 0; // used when generating zomRules

    // The state table with action and goto parts
    private final StateTable table = new StateTable ();

    public void addRule (String name, Object... os) {
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

    private void addRule (String name, List<SimplePart> parts) {
	Rule r = new Rule (name, rules.size (), parts);
	rules.add (r);
	RuleCollection rc = nameToRules.get (name);
	if (rc == null) {
	    rc = new RuleCollection ();
	    ruleCollections.add (rc);
	    nameToRules.put (name, rc);
	}
	rc.rules.add (r);
    }

    public ComplexPart zeroOrOne (String rule) {
	return new ZeroOrOneRulePart (new RulePart (rule));
    }

    public ComplexPart zeroOrOne (Token token) {
	return new ZeroOrOneRulePart (new TokenPart (token));
    }

    public ComplexPart zeroOrOne (Object... parts) {
	return new ZeroOrOneRulePart (sequence (parts));
    }

    public ComplexPart zeroOrMore (String rule) {
	return new ZeroOrMoreRulePart (new RulePart (rule));
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
		parts[i] = new RulePart ((String)os[i]);
	    else
		throw new IllegalArgumentException ("Unknown part: " + os[i]);
	}
	return parts;
    }

    private static class RuleCollection {
	private final List<Rule> rules;
	private boolean canBeEmpty = true;
	private EnumSet<Token> firsts;
	private EnumSet<Token> follow;

	public RuleCollection () {
	    rules = new ArrayList<> ();
	}
    }

    protected static class Rule {
	private final String name;
	private final int id;
	private final List<SimplePart> parts;

	public Rule (String name, int id, List<SimplePart> parts) {
	    this.name = name;
	    this.id = id;
	    this.parts = parts;
	}

	@Override public String toString () {
	    return name + " -> " + parts;
	}

	public String getName () {
	    return name;
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

	public int size () {
	    return parts.size ();
	}
    }

    private interface SimplePart {
	Object getId ();
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

	public T getId () {
	    return data;
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

    private class RulePart extends PartBase<String> {
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
	    RulePart newRule  = new RulePart ("ZOM_" + zomCounter++);
	    zomRules.put (part, newRule);
	    addRule (newRule.data, part);
	    addRule (newRule.data, new RulePart (newRule.data), part);
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
	    parts.clear (); // TODO: currently only handled at beginning
	    for (ComplexPart oop : this.parts) {
		List<List<SimplePart>> subRules = new ArrayList<> ();
		subRules.add (new ArrayList<> ());
		oop.split (subRules);
		parts.addAll (subRules);
	    }
	}
    }

    public void build () {
	System.out.println ("Validating rules; " + rules.size () + " / " + ruleCollections.size ());
	validateRules ();
	System.out.println ("Memorizing empty");
	memorizeEmpty ();
	System.out.println ("Memorizing firsts");
	memorizeFirsts ();
	System.out.println ("Memorizing follows");
	memorizeFollows ();
	System.out.println ("Building shift/goto");
	Map<ItemSet, Integer> itemSets = new HashMap<> ();
	Item startItem = new Item (rules.get (0), 0);
	ItemSet is = new ItemSet (Collections.singletonMap (startItem, EnumSet.of (END_OF_INPUT)));
	ItemSet s0 = closure1 (is);
	itemSets.put (s0, 0);
	table.addState (new StateRow (0));

	Queue<ItemSet> queue = new ArrayDeque<> ();
	queue.add (s0);

	while (!queue.isEmpty ()) {
	    ItemSet s = queue.remove ();
	    for (RuleCollection rc : ruleCollections)
		tryNextState (itemSets, queue, s, new RulePart (rc.rules.get (0).name));
	    for (Token t : Token.values ())
		tryNextState (itemSets, queue, s, new TokenPart (t));
	}
	System.out.println ("Adding reducde and accept");
	for (Map.Entry<ItemSet, Integer> me : itemSets.entrySet ()) {
	    ItemSet s = me.getKey ();
	    Integer sr = me.getValue ();

	    for (Map.Entry<Item, EnumSet<Token>> i2la : s.itemToLookAhead.entrySet ()) {
		Item i = i2la.getKey ();
		if (i.dotIsLast ()) {
		    int ruleId = i.r.id;
		    for (Token t : i2la.getValue ()) {
			if (t == Token.END_OF_INPUT && ruleId == 0)
			    table.get (sr).addAction (t, Action.createAccept ());
			else
			    table.get (sr).addAction (t, Action.createReduce (ruleId));
		    }
		}
	    }
	}
	System.out.println ("table:\n" + table.toTableString ());
    }

    private void tryNextState (Map<ItemSet, Integer> itemSets, Queue<ItemSet> queue,
			       ItemSet s, SimplePart sp) {
	ItemSet nextState = goTo (s, sp);
	if (nextState != null) {
	    StateRow sr = table.get (itemSets.get (s));
	    Integer i = itemSets.get (nextState);
	    if (i == null) {
		i = itemSets.size ();
		itemSets.put (nextState, i);
		table.addState (new StateRow (i));
		queue.add (nextState);
	    }
	    sr.addAction (sp.getId (), Action.createShift (i));
	}
    }

    private ItemSet goTo (ItemSet s, SimplePart sp) {
	Map<Item, EnumSet<Token>> sb = new HashMap<> ();
	for (Map.Entry<Item, EnumSet<Token>> me : s) {
	    Item i = me.getKey ();
	    if (i.hasNext (sp)) {
		Item next = i.advance ();
		sb.put (next, me.getValue ());
	    }
	}
	if (sb.isEmpty ())
	    return null;
	ItemSet isb = new ItemSet (sb);
	return closure1 (isb);
    }

    private void validateRules () {
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

    private void memorizeEmpty () {
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

    private boolean canBeEmpty (RuleCollection rc) {
	for (Rule r : rules) {
	    if (r.parts.isEmpty ())
		return true;
	    if (r.parts.stream ().allMatch (sp -> sp.canBeEmpty ()))
		return true;
	}
	return false;
    }

    private void memorizeFirsts () {
	ruleCollections.forEach (rc -> rc.firsts = EnumSet.noneOf (Token.class));
	boolean thereWasChanges;
	do {
	    thereWasChanges = false;
	    for (RuleCollection rc : ruleCollections)
		thereWasChanges |= rc.firsts.addAll (getFirsts (rc));
	} while (thereWasChanges);
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

    private void memorizeFollows () {
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
    }

    private boolean addFollow (String rule, SimplePart sp) {
	if (sp instanceof TokenPart) {
	    EnumSet<Token> ts = EnumSet.of (((TokenPart)sp).data);
	    return addFollow (rule, ts);
	}
	RulePart rp = (RulePart)sp;
	return addFollow (rule, nameToRules.get (rp.data).firsts);
    }

    private boolean addFollow (String rule, EnumSet<Token> ts) {
	return nameToRules.get (rule).follow.addAll (ts);
    }

    private ItemSet closure1 (ItemSet s) {
	boolean thereWasChanges;
	ItemSet res;
	do {
	    res = new ItemSet (s);
	    thereWasChanges = false;
	    for (Map.Entry<Item, EnumSet<Token>> me : s) {
		Item i = me.getKey ();
		EnumSet<Token> mlookAhead = me.getValue ();

		if (i.r.parts.size () <= i.dotPos)
		    continue;
		SimplePart symbolRightOfDot = i.r.getRulePart (i.dotPos);
		EnumSet<Token> lookAhead = EnumSet.noneOf (Token.class);
		boolean empty = true;
		for (SimplePart p : i.r.getPartsAfter (i.dotPos + 1)) {
		    lookAhead.addAll (p.getFirsts ());
		    if (!p.canBeEmpty ()) {
			empty = false;
			break;
		    }
		}
		if (empty)
		    lookAhead.addAll (mlookAhead);
		if (symbolRightOfDot instanceof RulePart) {
		    RulePart rp = (RulePart)symbolRightOfDot;
		    List<Rule> nrs = nameToRules.get (rp.data).rules;
		    for (Rule nr : nrs) {
			Item ni = new Item (nr, 0);
			thereWasChanges |= res.add (ni, lookAhead);
		    }
		}
	    }
	    s = res;
	} while (thereWasChanges);
	return new ItemSet (res);
    }

    private static class ItemSet implements Iterable<Map.Entry<Item, EnumSet<Token>>> {
	private final Map<Item, EnumSet<Token>> itemToLookAhead;

	public ItemSet (Map<Item, EnumSet<Token>> itemToLookAhead) {
	    this.itemToLookAhead = itemToLookAhead;
	}

	public ItemSet (ItemSet s) {
	    itemToLookAhead = new HashMap<> (s.itemToLookAhead);
	}

	public Iterator<Map.Entry<Item, EnumSet<Token>>> iterator () {
	    return itemToLookAhead.entrySet ().iterator ();
	}

	public boolean add (Item ni, EnumSet<Token> lookAhead) {
	    EnumSet<Token> la = itemToLookAhead.get (ni);
	    if (la == null) {
		itemToLookAhead.put (ni, lookAhead);
		return true;
	    }
	    return la.addAll (lookAhead);
	}

	@Override public String toString () {
	    return "ItemSet" + itemToLookAhead;
	}

	@Override public int hashCode () {
	    return itemToLookAhead.hashCode ();
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () == getClass ())
		return itemToLookAhead.equals (((ItemSet)o).itemToLookAhead);
	    return false;
	}
    }

    private static class Item {
	private final Rule r;
	private final int dotPos;

	public Item (Rule r, int dotPos) {
	    this.r = r;
	    this.dotPos = dotPos;
	}

	@Override public String toString () {
	    return "[" + r + ", dotPos: " + dotPos + "]";
	}

	@Override public int hashCode () {
	    return r.hashCode () + dotPos;
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () != getClass ())
		return false;
	    Item i = (Item)o;
	    return dotPos == i.dotPos && r.equals (i.r);
	}

	public boolean hasNext (SimplePart sp) {
	    if (r.parts.size () <= dotPos)
		return false;
	    return r.parts.get (dotPos).equals (sp);
	}

	public Item advance () {
	    return new Item (r, dotPos + 1);
	}

	public boolean dotIsLast () {
	    return dotPos == r.parts.size ();
	}
    }

    public Action getAction (int currentState, Token nextToken) {
	return table.getAction (currentState, nextToken);
    }

    public Integer getGoTo (int state, String rule) {
	return table.getGoTo (state, rule);
    }

    public List<Rule> getRules () {
	return rules;
    }
}
