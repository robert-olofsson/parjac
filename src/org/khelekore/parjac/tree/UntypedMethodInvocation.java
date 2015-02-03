package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class UntypedMethodInvocation implements TreeNode {
    private final String id;
    private final ArgumentList args;

    public UntypedMethodInvocation (Rule r, Deque<TreeNode> parts) {
	id = ((Identifier)parts.pop ()).get ();
	args = r.size () > 3 ? (ArgumentList)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + "(" + args + ")}";
    }

    public void visit (TreeVisitor visitor) {
	if (args != null)
	    args.visit (visitor);
    }
}
