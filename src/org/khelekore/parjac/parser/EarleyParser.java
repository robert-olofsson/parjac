package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.grammar.TokenPart;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.BooleanLiteral;
import org.khelekore.parjac.tree.CharLiteral;
import org.khelekore.parjac.tree.DoubleLiteral;
import org.khelekore.parjac.tree.FloatLiteral;
import org.khelekore.parjac.tree.Identifier;
import org.khelekore.parjac.tree.IntLiteral;
import org.khelekore.parjac.tree.LongLiteral;
import org.khelekore.parjac.tree.NullLiteral;
import org.khelekore.parjac.tree.StringLiteral;
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
    private final List<EarleyState> states = new ArrayList<> ();
    private int attemptedRecoveries;

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
	EarleyState es = new EarleyState (null);
	es.addState (new State (goalRule, 0, currentPosition));
	if (grammar.getRules ("Goal").canBeEmpty ())
	    es.addState (new State (goalRule, 1, currentPosition));
	states.add (es);
	Token nextToken = Token.END_OF_INPUT;
	TreeNode currentTokenValue = null;
	while (lexer.hasMoreTokens ()) {
	    nextToken = lexer.nextNonWhitespaceToken ();
	    if (treeBuilder != null)
		currentTokenValue = treeBuilder.getTokenValue (lexer, nextToken);
	    boolean recovery = false;
	    do {
		handleToken (currentPosition, nextToken, currentTokenValue);
		currentPosition++;
		if (states.size () <= currentPosition) {
		    addPossibleNextTokens (currentPosition - 1);
		    attemptedRecoveries++;
		    recovery = true;
		} else {
		    recovery = false;
		}
		if (states.size () <= currentPosition || attemptedRecoveries > 10) {
		    addParserError ("No possible next state");
		    return null;
		}
	    } while (recovery);
	}

	EarleyState finishingStates = states.get (currentPosition);
	if (debug)
	    System.err.println ("finishingStates: " + finishingStates);
	List<State> finished =
	    finishingStates.getStates ().stream ().filter (s -> s.dotIsLast ()).collect (Collectors.toList ());
	if (finished.isEmpty ()) {
	    addParserError ("Did not find any finishing state");
	} else {
	    if (finished.size () > 1)
		addParserError ("Ended up in many states: " + finishingStates);
	    else if (!isEndState (finished.get (0)))
		addParserError ("Ended up in wrong state: " + finishingStates);
	}
	if (diagnostics.hasError ())
	    return null;

	if (attemptedRecoveries > 0)
	    addParserError ("Attempted " + attemptedRecoveries + " parse recovieries");

	if (treeBuilder == null)
	    return null;

	SyntaxTree sn = buildTree (finished.get (0).getPrevious ()); // skip end of file
	if (debug)
	    System.err.println ("Built tree: " + sn);
	return sn;
    }

    private void handleToken (int currentPosition, Token nextToken, TreeNode currentTokenValue) {
	if (debug) {
	    if (currentTokenValue != null)
		System.err.println ("nextToken: " + nextToken + ": " + currentTokenValue);
	    else
		System.err.println ("nextToken: " + nextToken);
	}
	EarleyState current = states.get (currentPosition);
	current.setParsePosition (lexer.getParsePosition ());
	if (debug)
	    System.err.println (currentPosition + ": start current: " + current);
	completeState (current);
	predict (current);
	EarleyState sms = scan (current, currentPosition, nextToken, currentTokenValue);
	if (debug)
	    System.err.println (currentPosition + ": final current: " + current);
	if (sms != null)
	    states.add (sms);
    }

    private void completeState (EarleyState current) {
	Map<State, State> seen = new HashMap<> ();
	List<State> completed = new ArrayList<> ();
	Deque<State> multiComplete = new ArrayDeque<> ();
	for (State s : current.getStates ()) {
	    if (s.dotIsLast ())
		completeLast (s, seen, completed, multiComplete);
	}
	while (!multiComplete.isEmpty ())
	    completeLast (multiComplete.removeFirst (), seen, completed, multiComplete);
	current.addStates (completed);
    }

    private void completeLast (State completed, Map<State, State> seen,
			   List<State> allCompleted, Deque<State> multiComplete) {
	EarleyState originStates = states.get (completed.getStartPos ());
	for (State s : originStates.getStates ()) {
	    if (!s.dotIsLast () && s.getPartAfterDot ().getId ().equals (completed.getRule ().getName ())) {
		completeWithNext (completed, seen, allCompleted, multiComplete, s.advance (completed));
	    }
	}
	ListRuleHolder lrh = originStates.getListRuleHolder ();
	if (lrh != null) {
	    for (Iterator<Rule> i = lrh.getRulesWithRuleNext (completed.getRule ()); i.hasNext (); ) {
		State previousState = new State (i.next (), 0, completed.getStartPos ());
		completeWithNext (completed, seen, allCompleted, multiComplete, previousState.advance (completed));
	    }
	}
    }

    private void completeWithNext (State completed, Map<State, State> seen,
			   List<State> allCompleted, Deque<State> multiComplete, State nextState) {
	State alreadySeen = seen.get (nextState);
	if (alreadySeen == null) {
	    seen.put (nextState, nextState);
	    allCompleted.add (nextState);
	    if (nextState.dotIsLast ())
		multiComplete.add (nextState);
	} else {
	    alreadySeen.addCompleted (completed);
	}

	if (grammar.isClearableRule (completed.getRule ())) {
	    clearStates (completed);
	}
    }

    private void clearStates (State tc) {
	Deque<State> toVisit = new ArrayDeque<> ();
	toVisit.add (tc);

	// States in current EarleySet to keep, may be many due to completion
	Set<State> toKeep = new HashSet<> ();
	int tokenPos = states.size () - 1; // current pos
	while (!toVisit.isEmpty ()) {
	    EarleyState es = states.get (tokenPos);
	    State s = toVisit.pop ();
	    if (!es.hasBeenCleared ())
		toKeep.add (s);
	    State previous = s.getPrevious ();
	    if (previous != null) {
		toVisit.push (previous);
		if (previous.getPartAfterDot () instanceof TokenPart) {
		    if (!es.hasBeenCleared ()) {
			es.getStates ().retainAll (toKeep);
			toKeep.clear ();
			es.setCleared ();
		    }
		    tokenPos--;
		}
		List<State> completed = s.getCompleted ();
		if (completed != null)
		    for (State c : completed)
			toVisit.push (c);
	    }
	}
    }

    private void predict (EarleyState current) {
	Set<String> rules = new HashSet<> ();
	for (State s : current.getStates ()) {
	    if (!s.dotIsLast ()) {
		SimplePart sp = s.getPartAfterDot ();
		if (sp.isRulePart ())
		    rules.add ((String)sp.getId ());
	    }
	}
	current.setPredictedStates (predictCache.getPredictedRules (rules));
    }

    private EarleyState scan (EarleyState current, int currentPosition,
			      Token nextToken, TreeNode currentTokenValue) {
	EarleyState ret = new EarleyState (currentTokenValue);
	for (State s : current.getStates ()) {
	    if (!s.dotIsLast () && s.getPartAfterDot ().getId ().equals (nextToken))
		ret.addState (s.advance (null));
	}
	ListRuleHolder lrh = current.getListRuleHolder ();
	if (lrh != null) {
	    for (Iterator<Rule> i = lrh.getRulesWithTokenNext (nextToken); i.hasNext (); ) {
		State s = new State (i.next (), 0, currentPosition);
		current.addState (s);
		ret.addState (s.advance (null));
	    }
	}
	if (ret.isEmpty ())
	    return null;
	return ret;
    }

    private void addPossibleNextTokens (int currentPosition) {
	EarleyState es = states.get (states.size () - 1);
	EarleyState current = states.get (currentPosition);
	EarleyState next = null;
	for (Token t : es.getPossibleNextToken ()) {
	    EarleyState sms = scan (current, currentPosition, t, new ErrorTreeNode (lexer.getParsePosition ()));
	    if (sms != null) {
		if (next == null) {
		    next = sms;
		    states.add (next);
		} else {
		    next.addStates (sms.getStates ());
		}
	    }
	}
    }

    private boolean isEndState (State s) {
	return s.getStartPos () == 0 && s.dotIsLast () && s.getRule ().getName ().equals ("Goal");
    }

    private SyntaxTree buildTree (State s) {
	Deque<State> toVisit = new ArrayDeque<> ();
	toVisit.push (s);
	Deque<TreeNode> parts = new ArrayDeque<> ();
	Deque<SourceDiagnostics> errors = new ArrayDeque<> (); // since errors come in wrong order
	buildTreeNode (toVisit, parts, errors);
	errors.forEach (d -> diagnostics.report (d));
	if (parts.size () == 0)
	    return null;
	if (parts.size () != 1)
	    addParserError ("Got many parts back: " + parts);
	TreeNode topNode = parts.poll ();
	if (topNode == null)
	    return null;
	return new SyntaxTree (path, topNode);
    }

    private void buildTreeNode (Deque<State> toVisit, Deque<TreeNode> parts, Deque<SourceDiagnostics> errors) {
	int tokenPos = states.size () - 2; // skip <end_of_input>
	while (!toVisit.isEmpty ()) {
	    State s = toVisit.pop ();

	    State previous = s.getPrevious ();
	    EarleyState es = states.get (tokenPos);
	    if (previous == null) {
		treeBuilder.build (s, parts, es.getParsePosition (), path, diagnostics);
		continue;
	    } else {
		toVisit.push (previous);
		if (previous.getPartAfterDot () instanceof TokenPart) {
		    TreeNode tn = es.getTokenValue ();
		    if (tn instanceof ErrorTreeNode) {
			errors.push (SourceDiagnostics.error (path, tn.getParsePosition (),
							      "Missing: %s", previous.getPartAfterDot ()));
			tn = getErrorNode (tn.getParsePosition (), (Token)previous.getPartAfterDot ().getId ());
		    }
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

    private TreeNode getErrorNode (ParsePosition pos, Token token) {
	switch (token) {
	case INT_LITERAL:
	    return new IntLiteral (1, pos);
	case LONG_LITERAL:
	    return new LongLiteral (1, pos);
	case FLOAT_LITERAL:
	    return new FloatLiteral (1.0f, pos);
	case DOUBLE_LITERAL:
	    return new DoubleLiteral (1.0, pos);
	case CHARACTER_LITERAL:
	    return new CharLiteral ('c', pos);
	case STRING_LITERAL:
	    return new StringLiteral ("<string>", pos);
	case TRUE:
	    return new BooleanLiteral (true, pos);
	case FALSE:
	    return new BooleanLiteral (false, pos);
	case IDENTIFIER:
	    return new Identifier ("<identifier>", pos);
	case NULL:
	    return new NullLiteral (pos);
	default:
	    return null;
	}
    }

    private void addParserError (String error) {
	diagnostics.report (SourceDiagnostics.error (path, lexer.getParsePosition (), error));
    }
}