package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class Block extends PositionNode {
    private final List<TreeNode> statements;

    public Block (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	statements = r.size () > 2 ? ((BlockStatements)parts.pop ()).get () : null;
    }

    public Block (List<TreeNode> statements) {
	super (null);
	this.statements = statements;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + statements + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this) && statements != null)
	    statements.forEach (s -> s.visit (visitor));
	visitor.endBlock ();
    }

    public List<? extends TreeNode> getChildNodes () {
	if (statements == null)
	    return Collections.emptyList ();
	return statements;
    }

    public List<TreeNode> getStatements () {
	return statements;
    }

    public boolean lastIsNotThrowOrReturn () {
	if (statements == null)
	    return true;
	TreeNode tn = statements.get (statements.size () - 1);
	return !(tn instanceof ReturnStatement || tn instanceof ThrowStatement);
    }
}
