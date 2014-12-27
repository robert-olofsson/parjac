package org.khelekore.parjac.parser;

import java.util.ArrayDeque;
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

import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.RulePart;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.lexer.Token;

import static org.khelekore.parjac.lexer.Token.*;

public class LRParser {
    private final Grammar grammar;

    // The state table with action and goto parts
    private final StateTable table = new StateTable ();

    // Useful to turn on or off debugging for now, maybe we want to log instead?
    private final boolean debug;

    public LRParser (Grammar grammar, boolean debug) {
	this.grammar = grammar;
	this.debug = debug;
    }

    public Grammar getGrammar () {
	return grammar;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{#rules: " + grammar.getRules ().size () + ", debug: " + debug + "}";
    }

    public void build () {
	long start = System.nanoTime ();
	if (debug) {
	    int i = 0;
	    for (Rule r : grammar.getRules ()) {
		debug ("%4d: %s", i++, r);
	    }
	}
	debug ("Validating rules");
	grammar.validateRules ();
	grammar.memorize ();
	debug ("Building shift/goto");
	Map<ItemSet, Integer> itemSets = new HashMap<> ();
	Rule goalRule = grammar.getRules ("Goal").getRules ().get (0);
	Item startItem = new Item (goalRule, 0);
	ItemSet is = new ItemSet (Collections.singletonMap (startItem, EnumSet.of (END_OF_INPUT)));
	ItemSet s0 = closure1 (is);
	itemSets.put (s0, 0);
	table.addState (new StateRow (0));

	Queue<ItemSet> queue = new ArrayDeque<> ();
	queue.add (s0);

	while (!queue.isEmpty ()) {
	    ItemSet s = queue.remove ();
	    addGoTo (itemSets, queue, s);
	}
	debug ("Adding reducde and accept");
	int loopCounter = 0;
	int reduceConflictCount = 0;
	for (Map.Entry<ItemSet, Integer> me : itemSets.entrySet ()) {
	    ItemSet s = me.getKey ();
	    Integer sr = me.getValue ();
	    StateRow row = table.get (sr);

	    for (Map.Entry<Item, EnumSet<Token>> i2la : s.itemToLookAhead.entrySet ()) {
		Item i = i2la.getKey ();
		if (i.dotIsLast ()) {
		    int ruleId = i.r.getId ();
		    EnumSet<Token> reduceReduceConflicts = null;
		    for (Token t : i2la.getValue ()) {
			if (t == Token.END_OF_INPUT && ruleId == goalRule.getId ()) {
			    row.addAction (t, Action.createAccept ());
			} else {
			    // Do not overwrite shifts
			    Action a = row.getAction (t);
			    if (a == null) {
				row.addAction (t, Action.createReduce (ruleId));
			    } else {
				if (a.getType () == Action.Type.REDUCE) {
				    if (reduceReduceConflicts == null)
					reduceReduceConflicts = EnumSet.noneOf (Token.class);
				    reduceReduceConflicts.add (t);
				}
			    }
			}
		    }
		    if (reduceReduceConflicts != null) {
			System.out.format ("%4d: Got a reduce conflict: %4d, " +
					   "complete items: %s, lookahead tokens: %s, " +
					   "row id: %d\n",
					   ++reduceConflictCount, row.getId (),
					   s.getItemsWithDotLast (), reduceReduceConflicts,
					   sr);
		    }
		}
	    }
	    loopCounter++;
	    if (loopCounter % 1000 == 0)
		debug ("loopCounter: " + loopCounter);
	}
	long end = System.nanoTime ();
	if (debug) {
	    debug ("Total number of rules: %d\n" +
		   "State table with %d states built in %.3f seconds\n",
		   grammar.getRules ().size (), table.size (), (end - start) / 1e9);
	}
    }

    private void addGoTo (Map<ItemSet, Integer> itemSets, Queue<ItemSet> queue, ItemSet s) {
	Map<SimplePart, Map<Item, EnumSet<Token>>> sp2sb = new HashMap<> ();
	for (Map.Entry<Item, EnumSet<Token>> me : s) {
	    Item i = me.getKey ();
	    if (i.dotIsLast ())
		continue;
	    SimplePart sp = i.getPartAfterDot ();
	    Map<Item, EnumSet<Token>> sb = sp2sb.get (sp);
	    if (sb == null)
		sp2sb.put (sp, sb = new HashMap<> ());
	    sb.put (i.advance (), me.getValue ());
	}
	StateRow sr = table.get (itemSets.get (s));
	/* Can be usefule when debugging */
	/*
	if (debug)
	    debug (sr.getId () + ": " + s);
	*/
	for (Map.Entry<SimplePart, Map<Item, EnumSet<Token>>> me : sp2sb.entrySet ()) {
	    SimplePart sp = me.getKey ();
	    ItemSet isb = new ItemSet (me.getValue ());
	    ItemSet nextState = closure1 (isb);
	    Integer i = itemSets.get (nextState);
	    if (i == null) {
		i = itemSets.size ();
		itemSets.put (nextState, i);
		table.addState (new StateRow (i));
		queue.add (nextState);
	    }
	    /*
	    if (debug)
		debug (sp + " -> " + i);
	    */
	    sr.addAction (sp.getId (), Action.createShift (i));
	}
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

		if (i.r.getParts ().size () <= i.dotPos)
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
		    List<Rule> nrs = grammar.getRules (rp.getId ()).getRules ();
		    for (Rule nr : nrs) {
			Item ni = new Item (nr, 0);
			thereWasChanges |= res.add (ni, lookAhead);
		    }
		}
	    }
	    s = res;
	} while (thereWasChanges);
	return res;
    }

    private static class ItemSet implements Iterable<Map.Entry<Item, EnumSet<Token>>> {

	private final Map<Item, EnumSet<Token>> itemToLookAhead;
	private int hc = 0;

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
		hc = 0;
		return true;
	    }
	    return la.addAll (lookAhead);
	}

	@Override public String toString () {
	    return "ItemSet" + itemToLookAhead.keySet ();
	}

	@Override public int hashCode () {
	    if (hc != 0)
		return hc;
	    return hc = itemToLookAhead.hashCode ();
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

	public Set<Item> getItemsWithDotLast () {
	    return itemToLookAhead.keySet ().stream ().
		filter (i -> i.dotIsLast ()).
		collect (Collectors.toSet ());
	}
    }

    private static class Item {
	private final Rule r;
	private final int dotPos;
	private final int hc;

	public Item (Rule r, int dotPos) {
	    this.r = r;
	    this.dotPos = dotPos;
	    hc = r.hashCode () + dotPos;
	}

	@Override public String toString () {
	    return "[" + r + ", dotPos: " + dotPos + "]";
	}

	@Override public int hashCode () {
	    return hc;
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

	public SimplePart getPartAfterDot () {
	    return r.getParts ().get (dotPos);
	}

	public Item advance () {
	    return new Item (r, dotPos + 1);
	}

	public boolean dotIsLast () {
	    return dotPos == r.getParts ().size ();
	}
    }

    public Action getAction (int state, Token nextToken) {
	return table.getAction (state, nextToken);
    }

    public Collection<Token> getPossibleNextTokens (int state) {
	return table.getPossibleNextTokens (state);
    }

    public Integer getGoTo (int state, String rule) {
	return table.getGoTo (state, rule);
    }

    public List<Rule> getRules () {
	return grammar.getRules ();
    }

    public boolean getDebug () {
	return debug;
    }

    private void debug (String line) {
	if (debug) {
	    System.out.println (line);
	}
    }

    private void debug (String format, Object... params) {
	if (debug) {
	    System.out.format (format, params);
	    System.out.println ();
	}
    }
}
