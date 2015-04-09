package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class FieldAccess implements TreeNode {
    private final TreeNode from;
    private final boolean isSuper;
    private final String id;

    public FieldAccess (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	int pos = 0;
	if (r.getRulePart (0).isRulePart ()) {
	    from = parts.pop ();
	    pos++;
	} else {
	    from = null;
	}
	isSuper = r.getRulePart (pos).getId () == Token.SUPER;
	id = ((Identifier)parts.pop ()).get ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + from + " " + isSuper + " " + id + "}";
    }
}
