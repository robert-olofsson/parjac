package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

	return buildParseTree (finished.get (0).getPrevious ()); // skip end of file
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
	MultiState sms = scan (currentStates, cms, pms, currentPosition, nextToken);
	if (debug)
	    System.err.println (currentPosition + ": current: " + currentStates +
				", cms: " + cms + ", pms: " + pms);
	states.set (currentPosition, MergedMultiState.get (currentStates, cms, pms));
	if (sms != null)
	    states.add (sms);
    }

    private MultiState complete (MultiState currentStates) {
	Map<State, State> seen = new HashMap<> ();
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

    private void complete (State s, Map<State, State> seen,
			   List<State> completed, Deque<State> multiComplete) {
	MultiState originStates = states.get (s.getStartPos ());
	for (Iterator<State> os = originStates.getRulesWithNext (s.getRule ()); os.hasNext (); ) {
	    State nextState = os.next ().advance (s);
	    State alreadySeen = seen.get (nextState);
	    if (alreadySeen == null) {
		seen.put (nextState, nextState);
		completed.add (nextState);
		if (nextState.dotIsLast ())
		    multiComplete.add (nextState);
	    } else {
		alreadySeen.addCompleted (s);
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

    private MultiState scan (MultiState currentStates, MultiState cms, MultiState pms,
			     int currentPosition, Token nextToken) {
	List<State> scanned = new ArrayList<> ();
	scan (currentStates, nextToken, scanned);
	if (cms != null)
	    scan (cms, nextToken, scanned);
	if (pms != null)
	    scan (pms, nextToken, scanned);
	if (scanned.isEmpty ())
	    return null;
	return new ListMultiState (scanned);
    }

    private void scan (MultiState ms, Token nextToken, List<State> scanned) {
	for (Iterator<State> i = ms.getRulesWithNext (nextToken); i.hasNext (); ) {
	    State s = i.next ();
	    scanned.add (s.advance (null));
	}
    }

    private boolean isEndState (State s) {
	return s.getStartPos () == 0 && s.dotIsLast () && s.getRule ().getName ().equals ("Goal");
    }

    private SyntaxTree buildParseTree (State s) {
	if (s.getDotPos () == 0)
	    return null;
	State previous;
	// TODO: stop using recursion
	do {
	    List<State> completed = s.getCompleted ();
	    previous = s.getPrevious ();
	    if (completed != null) {
		if (completed.size () > 1)
		    System.err.println ("found many completed: " + completed);
		for (State c : completed)
		    buildParseTree (c);
	    }
	} while ((s = previous) != null);
	return null;
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