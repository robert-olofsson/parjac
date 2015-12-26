package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ConstructorBody extends PositionNode {
    private final ExplicitConstructorInvocation eci;
    private final BlockStatements statements;

    public ConstructorBody (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
	int pos = 1;
	if (r.getRulePart (pos).getId ().equals ("ExplicitConstructorInvocation")) {
	    eci = (ExplicitConstructorInvocation)parts.pop ();
	    pos++;
	} else {
	    eci = null;
	}
	if (r.getRulePart (pos).getId ().equals ("BlockStatements")) {
	    statements = (BlockStatements)parts.pop ();
	} else {
	    statements = null;
	}
    }

    public ConstructorBody (ExplicitConstructorInvocation eci, BlockStatements statements) {
	super (null);
	this.eci = eci;
	this.statements = statements;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + eci + " " + statements + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    if (eci != null)
		eci.visit (visitor);
	    if (statements != null)
		statements.visit (visitor);
	}
    }

    public Collection<? extends TreeNode> getChildNodes () {
	ArrayList<TreeNode> ls = new ArrayList<> (2);
	if (eci != null)
	    ls.add (eci);
	if (statements != null)
	    ls.add (statements);
	return ls;
    }

    public ExplicitConstructorInvocation getConstructorInvocation () {
	return eci;
    }
}
