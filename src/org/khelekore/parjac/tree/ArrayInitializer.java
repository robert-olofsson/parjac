package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class ArrayInitializer implements TreeNode {
    private final VariableInitializerList ls;

    public ArrayInitializer (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	ls = r.size () > 2 && r.getRulePart (1).getId () != Token.COMMA ?
	    (VariableInitializerList)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ls + "}";
    }
}
