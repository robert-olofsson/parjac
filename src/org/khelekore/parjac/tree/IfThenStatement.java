package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class IfThenStatement extends PositionNode {
    private final TreeNode exp;
    private final TreeNode ifStatement;
    private final TreeNode elseStatement;

    public IfThenStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	exp = parts.pop ();
	ifStatement = parts.pop ();
	elseStatement = r.size () > 5 ? parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + exp + " " + ifStatement + " " + elseStatement + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    exp.visit (visitor);
	    ifStatement.visit (visitor);
	    if (elseStatement != null)
		elseStatement.visit (visitor);
	}
    }

    public TreeNode getExp () {
	return exp;
    }

    public TreeNode getIfStatement () {
	return ifStatement;
    }

    public TreeNode getElseStatement () {
	return elseStatement;
    }

}
