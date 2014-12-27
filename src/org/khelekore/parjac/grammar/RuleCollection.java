package org.khelekore.parjac.grammar;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.khelekore.parjac.lexer.Token;

public class RuleCollection {
    private final List<Rule> rules;
    private boolean canBeEmpty = true;
    private EnumSet<Token> firsts;
    private EnumSet<Token> follows;

    public RuleCollection () {
	rules = new ArrayList<> ();
    }

    public List<Rule> getRules () {
	return rules;
    }

    public boolean canBeEmpty () {
	return canBeEmpty;
    }

    public void setCanBeEmpty (boolean b) {
	canBeEmpty = b;
    }

    public EnumSet<Token> getFirsts () {
	return firsts;
    }

    public void setFirsts (EnumSet<Token> firsts) {
	this.firsts = firsts;
    }

    public EnumSet<Token> getFollows () {
	return follows;
    }

    public void setFollows (EnumSet<Token> follows) {
	this.follows = follows;
    }
}

