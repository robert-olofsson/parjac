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
import org.khelekore.parjac.grammar.TokenPart;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.CompilationUnit;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;

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

    // The tree builder
    private final JavaTreeBuilder treeBuilder;

    public EarleyParser (Grammar grammar, Path path, Lexer lexer,
			 PredictCache predictCache, JavaTreeBuilder treeBuilder,
			 CompilerDiagnosticCollector diagnostics,
			 boolean debug) {
	this.grammar = grammar;
	this.path = path;
	this.lexer = lexer;
	this.predictCache = predictCache;
	this.diagnostics = diagnostics;
	this.debug = debug;
	this.treeBuilder = treeBuilder;
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
	MultiState state = new ListMultiState (startStates, null);
	states.add (state);
	Token nextToken = Token.END_OF_INPUT;
	TreeNode currentTokenValue = null;
	while (lexer.hasMoreTokens ()) {
	    nextToken = lexer.nextNonWhitespaceToken ();
	    if (treeBuilder != null)
		currentTokenValue = treeBuilder.getTokenValue (lexer, nextToken);
	    handleToken (currentPosition, nextToken, currentTokenValue);
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

	if (treeBuilder == null)
	    return null;

	SyntaxTree sn = buildTree (finished.get (0).getPrevious ()); // skip end of file
	if (debug)
	    System.err.println ("Build tree: " + sn);
	return sn;
    }

    private void handleToken (int currentPosition, Token nextToken, TreeNode currentTokenValue) {
	if (debug) {
	    if (currentTokenValue != null)
		System.err.println ("nextToken: " + nextToken + ": " + currentTokenValue);
	    else
		System.err.println ("nextToken: " + nextToken);
	}
	MultiState currentStates = states.get (currentPosition);
	MultiState cms = complete (currentStates);
	MultiState pms = predict (currentStates, cms, currentPosition);
	MultiState sms = scan (currentStates, cms, pms, currentPosition,
			       nextToken, currentTokenValue);
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
	return new ListMultiState (completed, null);
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
	ListRuleHolder rh = predictCache.getPredictedRules (currentStates.getPredictRules (), crules);
	if (rh == null)
	    return null;
	return new PredictedMultiState (rh, currentPosition);
    }

    private MultiState scan (MultiState currentStates, MultiState cms, MultiState pms,
			     int currentPosition, Token nextToken, TreeNode currentTokenValue) {
	List<State> scanned = new ArrayList<> ();
	scan (currentStates, nextToken, scanned);
	if (cms != null)
	    scan (cms, nextToken, scanned);
	if (pms != null)
	    scan (pms, nextToken, scanned);
	if (scanned.isEmpty ())
	    return null;
	return new ListMultiState (scanned, currentTokenValue);
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

    private int tokenPos = 0;

    private SyntaxTree buildTree (State s) {
	tokenPos = states.size () - 2; // skip <end_of_input>
	Deque<State> toVisit = new ArrayDeque<> ();
	toVisit.push (s);
	Deque<TreeNode> parts = new ArrayDeque<> ();
	buildTreeNode (toVisit, parts);
	if (parts.size () == 0)
	    return null;
	if (parts.size () != 1)
	    addParserError ("Got many parts back: " + parts);
	TreeNode topNode = parts.poll ();
	if (topNode == null)
	    return null;
	return new SyntaxTree (path, topNode);
    }

    private void buildTreeNode (Deque<State> toVisit, Deque<TreeNode> parts) {
	while (!toVisit.isEmpty ()) {
	    State s = toVisit.pop ();

	    State previous = s.getPrevious ();
	    if (previous == null) {
		treeBuilder.build (s, parts);
		continue;
	    } else {
		toVisit.push (previous);
		if (previous.getPartAfterDot () instanceof TokenPart) {
		    MultiState ms = states.get (tokenPos);
		    TreeNode tn = ms.getParsedToken ();
		    if (tn != null)
			parts.push (tn);
		    tokenPos--;
		}
		List<State> completed = s.getCompleted ();
		if (completed != null)
		    for (State c : completed)
			toVisit.push (c);
	    }
	}
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