package org.khelekore.parjac.parser;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.TreeNode;

public interface MultiState {
    Iterator<State> getCompletedStates ();
    Set<String> getPredictRules ();
    Iterator<State> getRulesWithNext (Rule r);
    Iterator<State> getRulesWithNext (Token t);
    EnumSet<Token> getPossibleNextToken ();

    /** Get the token value that we scanned to get into this state.
     *  May be null if no token was scanned
     */
    TreeNode getParsedToken ();

    /** Get the parse position of this state */
    ParsePosition getParsePosition ();
}
