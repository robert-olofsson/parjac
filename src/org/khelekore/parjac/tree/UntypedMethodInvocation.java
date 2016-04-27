package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class UntypedMethodInvocation extends PositionNode {
    private final String id;
    private final ArgumentList args;

    public UntypedMethodInvocation (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	id = ((Identifier)parts.pop ()).get ();
	args = r.size () > 3 ? (ArgumentList)parts.pop () : null;
    }

    public UntypedMethodInvocation (String id, ArgumentList args) {
	super (null);
	this.id = id;
	this.args = args;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + "(" + args + ")}";
    }

    public void visit (TreeVisitor visitor) {
	if (args != null)
	    args.visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	if (args != null)
	    return Collections.singleton (args);
	return Collections.emptyList ();
    }

    public String getId () {
	return id;
    }

    public ArgumentList getArgumentList () {
	return args;
    }
}
