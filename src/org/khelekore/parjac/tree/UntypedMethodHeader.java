package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class UntypedMethodHeader implements TreeNode {
    private final TreeNode result;
    private final MethodDeclarator declarator;
    private final Throws thrown;

    public UntypedMethodHeader (Rule r, Deque<TreeNode> parts) {
	result = (Result)parts.pop ();
	declarator = (MethodDeclarator)parts.pop ();
	thrown = r.size () > 2 ? (Throws)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + result + " " + declarator + " " + thrown + "}";
    }

    public TreeNode getResult () {
	return result;
    }

    public String getMethodName () {
	return declarator.getId ();
    }
}
