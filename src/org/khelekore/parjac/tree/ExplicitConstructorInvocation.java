package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class ExplicitConstructorInvocation implements TreeNode {
    private final TreeNode type;
    private final TypeArguments types;
    private final Token where;
    private final ConstructorArguments args;

    public ExplicitConstructorInvocation (Rule r, Deque<TreeNode> parts) {
	int pos = 0;
	if (r.size () > 3) {
	    type = parts.pop ();
	    pos += 2;
	} else {
	    type = null;
	}
	if (r.getRulePart (pos).isRulePart ()) {
	    types = (TypeArguments)parts.pop ();
	    pos++;
	} else {
	    types = null;
	}
	where = (Token)r.getRulePart (pos).getId ();
	args = (ConstructorArguments)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + types + " " + where + " " + args + "}";
    }
}
