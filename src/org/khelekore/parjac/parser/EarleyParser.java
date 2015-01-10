package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.SyntaxTree;

public class EarleyParser {
    // The grammar we are using
    private final Grammar grammar;
    // The file we are parsing
    private final Path path;
    // The lexer we are using to get tokens
    private final Lexer lexer;
    private final PredictCache predictCache;
    // Compiler output
    private final CompilerDiagnosticCollector diagnostics;
    private final boolean debug;

    // The state table
    private final List<MultiState> states = new ArrayList<> ();

    public EarleyParser (Grammar grammar, Path path, Lexer lexer,
			 PredictCache predictCache,
			 CompilerDiagnosticCollector diagnostics,
			 boolean debug) {
	this.grammar = grammar;
	this.path = path;
	this.lexer = lexer;
	this.predictCache = predictCache;
	this.diagnostics = diagnostics;
	this.debug = debug;
	if (debug) {
	    int i = 0;
	    System.err.format ("%d rule names, %d rules\n",
			       grammar.getNumberOfRuleNames (),
			       grammar.getNumberOfRules ());
	    for (Rule r : grammar.getRules ())
		System.err.format ("%4d: %s\n", i++, r);
	}
    }

    public SyntaxTree parse () {
	Rule goalRule = grammar.getRules ("Goal").getRules ().get (0);
	int currentPosition = 0;
	List<State> startStates = new ArrayList<> ();
	startStates.add (new State (goalRule, 0, currentPosition));
	if (grammar.getRules ("Goal").canBeEmpty ())
	    startStates.add (new State (goalRule, 1, currentPosition));
	MultiState state = new ListMultiState (startStates);
	states.add (state);
	Token nextToken = Token.END_OF_INPUT;
	while (lexer.hasMoreTokens ()) {
	    nextToken = lexer.nextNonWhitespaceToken ();
	    handleToken (currentPosition, nextToken);
	    // TODO: debugPrintStates (currentPosition);
	    currentPosition++;
	    if (states.size () <= currentPosition) {
		addParserError ("No possible next state");
		return null;
	    }
	}

	ListMultiState finishingStates = (ListMultiState)states.get (currentPosition);
	if (debug)
	    System.err.println ("finishingStates: " + finishingStates);
	List<State> finished = finishingStates.getCompleted ();
	if (finished.size () > 1)
	    addParserError ("Ended up in many states: " + finishingStates);
	if (!isEndState (finished.get (0)))
	    addParserError ("Ended up in wrong state: " + finishingStates);
	return null;
    }

    private void handleToken (int currentPosition, Token nextToken) {
	if (debug) {
	    if (nextToken == Token.IDENTIFIER)
		System.err.println ("nextToken: " + nextToken + " \"" + lexer.getIdentifier () + "\"");
	    else
		System.err.println ("nextToken: " + nextToken);
	}
	MultiState currentStates = states.get (currentPosition);
	MultiState cms = complete (currentStates);
	MultiState pms = predict (currentStates, cms, currentPosition);
	MultiState sms = scan (currentStates, pms, cms, currentPosition, nextToken);
	if (debug)
	    System.err.println (currentPosition + ": current: " + currentStates +
				", pms: " + pms + ", cms: " + cms);
	states.set (currentPosition, MergedMultiState.get (currentStates, pms, cms));
	if (sms != null)
	    states.add (sms);
    }

    private MultiState complete (MultiState currentStates) {
	Set<State> seen = new HashSet<> ();
	List<State> completed = new ArrayList<> ();
	Deque<State> multiComplete = new ArrayDeque<> ();
	for (Iterator<State> is = currentStates.getCompletedStates (); is.hasNext (); )
	    complete (is.next (), seen, completed, multiComplete);
	while (!multiComplete.isEmpty ())
	    complete (multiComplete.removeFirst (), seen, completed, multiComplete);
	if (completed.isEmpty ())
	    return null;
	return new ListMultiState (completed);
    }

    private void complete (State s, Set<State> seen, List<State> completed, Deque<State> multiComplete) {
	MultiState originStates = states.get (s.getStartPos ());
	for (Iterator<State> os = originStates.getRulesWithNext (s.getRule ()); os.hasNext (); ) {
	    State c = os.next ().advance ();
	    if (seen.add (c)) {
		completed.add (c);
		if (c.dotIsLast ())
		    multiComplete.add (c);
	    }
	}
    }

    private MultiState predict (MultiState currentStates, MultiState cms, int currentPosition) {
	Set<String> crules = cms != null ? cms.getPredictRules () : Collections.emptySet ();
	ListRuleHolder rh = getPredictedRules (currentStates.getPredictRules (), crules);
	if (rh == null)
	    return null;
	return new PredictedMultiState (rh, currentPosition);
    }

    private ListRuleHolder getPredictedRules (Set<String> rules, Set<String> crules) {
	return predictCache.getPredictedRules (rules, crules);
    }

    private MultiState scan (MultiState currentStates, MultiState pms, MultiState cms,
			     int currentPosition, Token nextToken) {
	List<State> scanned = new ArrayList<> ();
	for (Iterator<State> s = currentStates.getRulesWithNext (nextToken); s.hasNext (); )
	    scanned.add (s.next ().advance ());
	if (pms != null) {
	    for (Iterator<State> s = pms.getRulesWithNext (nextToken); s.hasNext (); )
		scanned.add (s.next ().advance ());
	}
	if (cms != null) {
	    for (Iterator<State> s = cms.getRulesWithNext (nextToken); s.hasNext (); )
		scanned.add (s.next ().advance ());
	}
	if (scanned.isEmpty ())
	    return null;
	return new ListMultiState (scanned);
    }

    private boolean isEndState (State s) {
	return s.getStartPos () == 0 && s.dotIsLast () && s.getRule ().getName ().equals ("Goal");
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