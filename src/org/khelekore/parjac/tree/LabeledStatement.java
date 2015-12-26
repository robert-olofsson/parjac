package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class LabeledStatement extends PositionNode {
    private final String id;
    private final TreeNode statement;

    public LabeledStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	id = ((Identifier)parts.pop ()).get ();
	parts.pop (); // ':'
	statement = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + " : " + statement + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	statement.visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (statement);
    }
}
