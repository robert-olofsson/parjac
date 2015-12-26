package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class ExplicitConstructorInvocation extends PositionNode {
    private TreeNode type;
    private final TypeArguments types;
    private final Token where;
    private final ConstructorArguments args;

    public ExplicitConstructorInvocation (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
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

    public ExplicitConstructorInvocation (TypeArguments types, Token where, ConstructorArguments args) {
	super (null);
	this.types = types;
	this.where = where;
	this.args = args;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + types + " " + where + " " + args + "}";
    }

    public TreeNode getType () {
	return type;
    }

    public void setType (TreeNode type) {
	this.type = type;
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	args.visit (visitor);
	visitor.endExplicitConstructorInvocation (this);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (args);
    }
}
