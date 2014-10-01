package org.khelekore.parjac.parser;

import java.nio.file.Path;
import java.util.ArrayDeque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.SyntaxTree;

/** A parser for */
public class Parser {
    // The lr parser
    private final LRParser lr;

    // The file we are parsing
    private final Path path;

    // The lexer we are using to get tokens
    private final Lexer lexer;

    // Compiler output
    private final CompilerDiagnosticCollector diagnostics;

    // Stack of state item
    private ArrayDeque<Integer> stack = new ArrayDeque<> ();

    // Next token in the lexer
    private Token nextToken;

    private final boolean debug;

    public Parser (LRParser lr, Path path, Lexer lexer,
		   CompilerDiagnosticCollector diagnostics,
		   boolean debug) {
	this.lr = lr;
	this.path = path;
	this.lexer = lexer;
	nextToken = lexer.nextToken ();
	this.diagnostics = diagnostics;
	this.debug = debug;
    }

    public SyntaxTree parse () {
	stack.push (0);
	while (true) {
	    int currentState = stack.peek ();
	    Action a = lr.getAction (currentState, nextToken);
	    debug ("Stack: %s, currentState: %d, action: %s, nextToken: %s",
		   stack, currentState, a, nextToken);
	    if (nextToken == Token.ERROR) {
		addParserError (lexer.getError ());
		break;
	    }
	    if (a == null) {
		addParserError ("No action for nextToken: " + nextToken);
		break;
	    } else {
		switch (a.getType ()) {
		case SHIFT:
		    stack.push (a.getN ());
		    nextToken = lexer.nextNonWhitespaceToken ();
		    break;
		case REDUCE:
		    LRParser.Rule r = lr.getRules ().get (a.getN ());
		    debug ("reduce using %s", r);
		    for (int i = 0, s = r.size (); i < s; i ++) // pop the parts
			stack.pop ();
		    int topState = (Integer)stack.peek ();
		    Integer goTo = lr.getGoTo (topState, r.getName ());
		    stack.push (goTo);
		    break;
		case ACCEPT:
		    return new SyntaxTree (path); // TODO: not really correct
		case ERROR:
		    addParserError ("ERROR: for nextToken: " + nextToken);
		    break;
		}
	    }
	}
	return null;
    }

    public CompilerDiagnosticCollector getDiagnostics () {
	return diagnostics;
    }

    private void addParserError (String error) {
	diagnostics.report (new SourceDiagnostics (path,
						   lexer.getTokenStartPos (),
						   lexer.getTokenEndPos (),
						   lexer.getLineNumber (),
						   lexer.getTokenColumn (),
						   error));
    }

    private void debug (String format, Object... params) {
	if (debug) {
	    System.out.format (format, params);
	    System.out.println ();
	}
    }
}
