package org.khelekore.parjac.parser;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class ParseErrorSolution {
    private final Token wantedToken; // TODO: probably make this a List<Token>?
    private final Rule rule;

    public ParseErrorSolution (Token wantedToken, Rule rule) {
	this.wantedToken = wantedToken;
	this.rule = rule;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + wantedToken + ", " + rule + "}";
    }

    public Token getWantedToken () {
	return wantedToken;
    }

    public Rule getRule () {
	return rule;
    }
}