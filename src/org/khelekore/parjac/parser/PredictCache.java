package org.khelekore.parjac.parser;

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
    private final Map<String, ListRuleHolder> ruleToPredictRules;

    public PredictCache (Grammar grammar) {
	this.grammar = grammar;
	ruleToPredictRules = calculatePredictSets ();
    }

    public ListRuleHolder getPredictedRules (Set<String> rules, Set<String> crules) {
	ListRuleHolder predicted = getPredictedRules (rules);
	ListRuleHolder ppredicted = getPredictedRules (crules);
	if (predicted == null)
	    return ppredicted;
	return predicted.merge (ppredicted);
    }

    public ListRuleHolder getPredictedRules (Set<String> rules) {
	ListRuleHolder predicted = null;
	for (String rule : rules) {
	    ListRuleHolder pr = getPredictedRules (rule);
	    if (pr != null)
		predicted = pr.merge (predicted);
	}
	return predicted;
    }

    public ListRuleHolder getPredictedRules (String rule) {
	return ruleToPredictRules.get (rule);
    }

    private Map<String, ListRuleHolder> calculatePredictSets () {
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
	Map<String, ListRuleHolder> ret = new HashMap<> ();
	for (Map.Entry<String, Set<Rule>> me : ruleToPredictRules.entrySet ())
	    ret.put (me.getKey (), new ListRuleHolder (me.getValue ()));
	return ret;
    }
}