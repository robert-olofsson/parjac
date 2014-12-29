package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.RuleCollection;
import org.khelekore.parjac.grammar.RulePart;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.grammar.TokenPart;
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
    // Compiler output
    private final CompilerDiagnosticCollector diagnostics;
    private final boolean debug;

    // The state table
    private final List<StateSet> states = new ArrayList<> ();

    public EarleyParser (Grammar grammar, Path path, Lexer lexer,
			 CompilerDiagnosticCollector diagnostics,
			 boolean debug) {
	this.grammar = grammar;
	this.path = path;
	this.lexer = lexer;
	this.diagnostics = diagnostics;
	this.debug = debug;
	if (debug) {
	    int i = 0;
	    for (Rule r : grammar.getRules ())
		System.err.format ("%4d: %s\n", i++, r);
	}
    }

    public SyntaxTree parse () {
	Rule goalRule = grammar.getRules ("Goal").getRules ().get (0);
	int currentPosition = 0;
	StateSet startStates = new StateSet ();
	startStates.add (new State (goalRule, 0, currentPosition));
	states.add (startStates);
	Token nextToken = Token.END_OF_INPUT;
	while (lexer.hasMoreTokens ()) {
	    nextToken = lexer.nextNonWhitespaceToken ();
	    handleToken (currentPosition, nextToken);
	    debugPrintStates (currentPosition++);
	    if (states.size () <= currentPosition) {
		addParserError ("No possible next state");
		return null;
	    }
	}
	StateSet finishingStates = states.get (currentPosition);
	if (debug)
	    System.err.println ("finishingStates: " + finishingStates);
	if (finishingStates.size () == 1) {
	    State finish = finishingStates.get (0);
	    if (!isEndState (finish))
		addParserError ("Ended up in wrong state: " + finish);
	} else {
	    addParserError ("Ended up in many states: " + finishingStates);
	}
	return null;
    }

    private void handleToken (int currentPosition, Token nextToken) {
	if (debug) {
	    if (nextToken == Token.IDENTIFIER)
		System.err.println ("nextToken: " + nextToken + " \"" + lexer.getIdentifier () + "\"");
	    else
		System.err.println ("nextToken: " + nextToken);
	}
	StateSet currentStates = states.get (currentPosition);
	for (int s = 0; s < currentStates.size (); s++) {
	    State state = currentStates.get (s);
	    if (incomplete (state)) {
		SimplePart sp = state.getPartAfterDot ();
		if (sp instanceof TokenPart) {
		    TokenPart tp = (TokenPart)sp;
		    scan (nextToken, tp, state, currentPosition);
		} else {
		    RulePart rp = (RulePart)sp;
		    predict (state, rp, currentPosition, currentStates);
		}
	    } else {
		complete (state, currentPosition, currentStates);
	    }
	}
    }

    private boolean incomplete (State state) {
	return !state.dotIsLast ();
    }

    private void scan (Token nextToken, TokenPart afterDot, State state, int pos) {
	if (afterDot.getId () == nextToken) {
	    int nextPos = pos + 1;
	    StateSet nextStates;
	    if (states.size () <= nextPos) {
		nextStates = new StateSet ();
		states.add (nextStates);
	    } else {
		nextStates = states.get (nextPos);
	    }
	    nextStates.add (state.advance ());
	}
    }

    private void predict (State state, RulePart rp, int pos, StateSet states) {
	RuleCollection rc = grammar.getRules (rp.getId ());
	for (Rule r : rc.getRules ())
	    states.add (new State (r, 0, pos));
    }

    private void complete (State state, int pos, StateSet currentStates) {
	StateSet originStates = states.get (state.startPos);
	for (int i = 0; i < originStates.size (); i++) {
	    State os = originStates.get (i);
	    if (incomplete (os) && nextIsRule (os, state.getRule ().getName ()))
		currentStates.add (os.advance ());
	}
    }

    private boolean nextIsRule (State state, String ruleName) {
	SimplePart sp = state.getPartAfterDot ();
	if (sp instanceof RulePart) {
	    RulePart rp = (RulePart)sp;
	    return ruleName.equals (rp.getId ());
	}
	return false;
    }

    private boolean isEndState (State s) {
	return s.startPos == 0 && s.dotIsLast () && s.getRule ().getName ().equals ("Goal");
    }

    private void debugPrintStates (int pos) {
	if (debug)
	    states.get (pos).forEach (s -> System.err.println (pos + ": " + s));
    }

    private static class StateSet implements Iterable<State> {
	private final List<State> states = new ArrayList<> (100);
	private final Set<State> ss = new HashSet<> (100);

	public void add (State state) {
	    if (ss.add (state))
		states.add (state);
	}

	public int size () {
	    return states.size ();
	}

	public State get (int i) {
	    return states.get (i);
	}

	@Override public String toString () {
	    return states.toString ();
	}

	public Iterator<State> iterator () {
	    return states.iterator ();
	}
    }

    private static class State extends Item {
	private final int startPos;
	private final int hc;

	public State (Rule r, int dotPos, int startPos) {
	    super (r, dotPos);
	    this.startPos = startPos;
	    hc = startPos * 31 + super.hashCode ();
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () +
		"{" + getRule () + ", " + getDotPos () + ", " + startPos + "}";
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () != getClass ())
		return false;
	    State s = (State)o;
	    return startPos == s.startPos &&
		getDotPos () == s.getDotPos () &&
		getRule ().equals (s.getRule ());
	}

	@Override public int hashCode () {
	    return hc;
	}

	@Override public State advance () {
	    return new State (getRule (), getDotPos () + 1, startPos);
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