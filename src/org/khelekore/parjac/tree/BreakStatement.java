package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class BreakStatement extends PositionNode {
    private final String id;

    public BreakStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	id = r.size () > 2 ? ((Identifier)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }
}
