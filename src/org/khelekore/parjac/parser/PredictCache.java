package org.khelekore.parjac.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.RulePart;
import org.khelekore.parjac.grammar.SimplePart;

public class PredictCache {
    private final Grammar grammar;
    // Key is either String for plain rules or Set<String> for rule collections
    private final ConcurrentMap<Object, ListRuleHolder> cache;

    public PredictCache (Grammar grammar) {
	this.grammar = grammar;
	cache = calculatePredictSets ();
    }

    public ListRuleHolder getPredictedRules (Set<String> rules) {
	return cache.computeIfAbsent (rules, rs -> calculate (rules));
    }

    private ListRuleHolder calculate (Set<String> rules) {
	ListRuleHolder predicted = new ListRuleHolder (Collections.emptySet ());
	for (String rule : rules) {
	    ListRuleHolder pr = cache.get (rule);
	    if (pr != null)
		predicted.add (pr);
	}
	return predicted;
    }

    private ConcurrentMap<Object, ListRuleHolder> calculatePredictSets () {
	Map<String, Set<Rule>> ruleToPredictRules = new HashMap<> ();
	for (String rulename : grammar.getUniqueRuleNames ()) {
	    Set<Rule> rules = new HashSet<> ();
	    rules.addAll (grammar.getRules (rulename).getRules ());
	    ruleToPredictRules.put (rulename, rules);
	}

	boolean thereWasChange;
	do {
	    thereWasChange = false;
	    for (Rule rule : grammar.getRules ()) {
		if (rule.isEmpty ())
		    continue;
		String rulename = rule.getName ();
		Set<Rule> predictions = ruleToPredictRules.get (rulename);
		SimplePart sp = rule.getRulePart (0);
		if (sp instanceof RulePart) {
		    String predictRule = (String)sp.getId ();
		    if (predictRule != rulename) {
			thereWasChange |= predictions.addAll (grammar.getRules (predictRule).getRules ());
			thereWasChange |= predictions.addAll (ruleToPredictRules.get (predictRule));
		    }
		}
	    }
	} while (thereWasChange);
	ConcurrentMap<Object, ListRuleHolder> ret = new ConcurrentHashMap<> ();
	for (Map.Entry<String, Set<Rule>> me : ruleToPredictRules.entrySet ())
	    ret.put (me.getKey (), new ListRuleHolder (me.getValue ()));
	return ret;
    }
}