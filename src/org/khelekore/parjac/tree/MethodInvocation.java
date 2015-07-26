package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class MethodInvocation extends PositionNode {
    private final TreeNode on;
    private final TypeArguments types;
    private final boolean isSuper;
    private final UntypedMethodInvocation mi;

    public MethodInvocation (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
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

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 1)
	    return parts.pop ();
	return new MethodInvocation (r, parts, pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + on + " " + isSuper + " " + types + " " + mi + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    mi.visit (visitor);
    }

    public TreeNode getOn () {
	return on;
    }

    public TypeArguments getTypeArguments () {
	return types;
    }

    public boolean isSuper () {
	return isSuper;
    }

    public String getId () {
	return mi.getId ();
    }

    public ArgumentList getArgumentList () {
	return mi.getArgumentList ();
    }
}
