package org.khelekore.parjac.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.RulePart;
import org.khelekore.parjac.grammar.SimplePart;

public class PredictCache {
    private final Grammar grammar;
    private final Map<Object, ListRuleHolder> cache;

    public PredictCache (Grammar grammar) {
	this.grammar = grammar;
	cache = Collections.synchronizedMap (calculatePredictSets ());
    }

    public ListRuleHolder getPredictedRules (Set<String> rules, Set<String> crules) {
	if (crules.isEmpty ())
	    return getPredictedRules (rules);
	if (rules.isEmpty ())
	    return getPredictedRules (crules);
	Set<String> rr = new HashSet<> ();
	rr.addAll (rules);
	rr.addAll (crules);
	return getPredictedRules (rr);
    }

    public ListRuleHolder getPredictedRules (Set<String> rules) {
	ListRuleHolder predicted = cache.get (rules);
	if (predicted != null)
	    return predicted;

	for (String rule : rules) {
	    ListRuleHolder pr = cache.get (rule);
	    if (pr != null)
		predicted = pr.merge (predicted);
	}
	cache.put (rules, predicted);
	return predicted;
    }

    private Map<Object, ListRuleHolder> calculatePredictSets () {
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
		if (rule.size () == 0)
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
	Map<Object, ListRuleHolder> ret = new HashMap<> ();
	for (Map.Entry<String, Set<Rule>> me : ruleToPredictRules.entrySet ())
	    ret.put (me.getKey (), new ListRuleHolder (me.getValue ()));
	return ret;
    }
}