package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class MethodInvocation implements TreeNode {
    private final TreeNode on;
    private final TypeArguments types;
    private final boolean isSuper;
    private final UntypedMethodInvocation mi;

    public MethodInvocation (Rule r, Deque<TreeNode> parts) {
	int pos = 0;
	if (r.getRulePart (0).getId () == Token.SUPER) {
	    on = null;
	    isSuper = true;
	    pos += 2;
	} else if (r.size () > 4) {
	    on = parts.pop ();
	    isSuper = false;
	    pos += 4;
	} else {
	    isSuper = false;
	    on = parts.pop ();
	    pos += 2;
	}
	types = r.size () > pos + 1 ? (TypeArguments)parts.pop () : null;
	mi = (UntypedMethodInvocation)parts.pop ();
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    return parts.pop ();
	return new MethodInvocation (r, parts);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + on + " " + isSuper + " " + types + " " + mi + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (on != null)
	    on.visit (visitor);
	mi.visit (visitor);
    }
}
