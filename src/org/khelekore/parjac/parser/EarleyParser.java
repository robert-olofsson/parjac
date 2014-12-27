package org.khelekore.parjac.parser;

import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.RuleCollection;
import org.khelekore.parjac.grammar.RulePart;
import org.khelekore.parjac.grammar.SimplePart;
import org.khelekore.parjac.grammar.TokenPart;
import org.khelekore.parjac.lexer.CharBufferLexer;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;

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
    }

    public void parse () {
	Rule goalRule = grammar.getRules ("Goal").getRules ().get (0);
	Item startItem = new Item (goalRule, 0);
	int currentPosition = 0;
	StateSet startStates = new StateSet ();
	startStates.add (new State (startItem, currentPosition));
	states.add (startStates);
	while (lexer.hasMoreTokens ()) {
	    Token nextToken = lexer.nextNonWhitespaceToken ();
	    handleToken (currentPosition, nextToken);
	    debugPrintStates (currentPosition++);
	    if (states.size () <= currentPosition) {
		addParserError ("No possible next state");
		return;
	    }
	}
	handleToken (currentPosition, Token.END_OF_INPUT);
	debugPrintStates (currentPosition++);
	if (states.size () <= currentPosition) {
	    addParserError ("No possible next state");
	    return;
	}
	StateSet finishingStates = states.get (currentPosition);
	if (debug)
	    System.out.println ("finishingStates: " + finishingStates);
	if (finishingStates.size () == 1) {
	    State finish = finishingStates.get (0);
	    if (!isEndState (finish))
		addParserError ("Ended up in wrong state: " + finish);
	} else {
	    addParserError ("Ended up in many states: " + finishingStates);
	}
    }

    private void handleToken (int currentPosition, Token nextToken) {
	if (debug)
	    System.out.println ("nextToken: " + nextToken);
	StateSet currentStates = states.get (currentPosition);
	for (int s = 0; s < currentStates.size (); s++) {
	    State state = currentStates.get (s);
	    if (incomplete (state)) {
		if (nextIsToken (state)) {
		    scan (nextToken, state, currentPosition);
		} else {
		    predict (state, currentPosition, currentStates);
		}
	    } else {
		complete (state, currentPosition, currentStates);
	    }
	}
    }

    private boolean incomplete (State state) {
	return !state.item.dotIsLast ();
    }

    private boolean nextIsToken (State state) {
	return state.item.getPartAfterDot () instanceof TokenPart;
    }

    private void scan (Token nextToken, State state, int pos) {
	TokenPart tp = (TokenPart)state.item.getPartAfterDot ();
	if (tp.getId () == nextToken) {
	    int nextPos = pos + 1;
	    StateSet nextStates;
	    if (states.size () <= nextPos) {
		nextStates = new StateSet ();
		states.add (nextStates);
	    } else {
		nextStates = states.get (nextPos);
	    }
	    nextStates.add (new State (state.item.advance (), state.startPos));
	}
    }

    private void predict (State state, int pos, StateSet states) {
	RulePart rp = (RulePart)state.item.getPartAfterDot ();
	RuleCollection rc = grammar.getRules (rp.getId ());
	rc.getRules ().forEach (r -> states.add (new State (new Item (r, 0), pos)));
    }

    private void complete (State state, int pos, StateSet currentStates) {
	StateSet originStates = states.get (state.startPos);
	for (State os : originStates)
	    if (incomplete (os) && nextIsRule (os, state.item.getRule ().getName ()))
		currentStates.add (new State (os.item.advance (), os.startPos));
    }

    private boolean nextIsRule (State state, String ruleName) {
	SimplePart sp = state.item.getPartAfterDot ();
	if (sp instanceof RulePart) {
	    RulePart rp = (RulePart)sp;
	    return ruleName.equals (rp.getId ());
	}
	return false;
    }

    private boolean isEndState (State s) {
	return s.startPos == 0 && s.item.dotIsLast () && s.item.getRule ().getName ().equals ("Goal");
    }

    private void debugPrintStates (int pos) {
	if (debug)
	    states.get (pos).forEach (s -> System.out.println (pos + ": " + s));
    }

    private static class StateSet implements Iterable<State> {
	private final List<State> states = new ArrayList<> ();

	public void add (State state) {
	    if (!states.stream ().anyMatch (s -> s.equals (state)))
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

    private static class State {
	private final Item item;
	private final int startPos;

	public State (Item item, int startPos) {
	    this.item = item;
	    this.startPos = startPos;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + item + ", " + startPos + "}";
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () != getClass ())
		return false;
	    State s = (State)o;
	    return startPos == s.startPos && item.equals (s.item);
	}

	@Override public int hashCode () {
	    return startPos * 31 + item.hashCode ();
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

    public static void main (String[] args) {
	Grammar g = new Grammar ();
	g.addRule ("Goal", "S", Token.END_OF_INPUT);
	g.addRule ("S", "NP", "VP");
	g.addRule ("NP", Token.PLUS, Token.GT, Token.IDENTIFIER);
	g.addRule ("NP", Token.PLUS, Token.IDENTIFIER);
	g.addRule ("NP", Token.GT, Token.IDENTIFIER);
	g.addRule ("VP", Token.AT, "VP");
	g.addRule ("VP", Token.MULTIPLY, "NP");
	g.validateRules ();
	g.memorize ();

	CharBuffer cb = CharBuffer.wrap ("+ > noun @ * + noun2");
	Path path = Paths.get ("EarleyParser");
	Lexer lexer = new CharBufferLexer (path, cb);
	CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	EarleyParser ep = new EarleyParser (g, path, lexer, diagnostics, true);
	ep.parse ();
	if (diagnostics.hasError ())
	    System.err.println ("Failed to parse correctly");
    }
}