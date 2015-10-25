package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ConstructorArguments extends PositionNode {
    private final ArgumentList args;

    public ConstructorArguments (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	args = r.size () > 3 ? (ArgumentList)parts.pop () : null;
    }

    public ConstructorArguments (ArgumentList args) {
	super (null);
	this.args = args;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{(" + args + ");}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (args != null)
	    args.visit (visitor);
    }
}
