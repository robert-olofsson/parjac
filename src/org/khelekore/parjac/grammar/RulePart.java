package org.khelekore.parjac.grammar;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import org.khelekore.parjac.lexer.Token;

public class RulePart extends PartBase<String> {
    private final Grammar g;

    public RulePart (String rule, Grammar g) {
	super (rule);
	this.g = g;
    }

    @Override public Collection<String> getSubrules () {
	return Collections.singleton (data);
    }

    @Override public boolean canBeEmpty () {
	return g.getRules (data).canBeEmpty ();
    }

    @Override public EnumSet<Token> getFirsts () {
	return g.getRules (data).getFirsts ();
    }

    @Override public boolean isRulePart () {
	return true;
    }

    @Override public boolean isTokenPart () {
	return false;
    }
}
