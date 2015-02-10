package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class MethodDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final MethodHeader header;
    private final MethodBody body;

    public MethodDeclaration (Rule r, Deque<TreeNode> parts) {
	modifiers = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	header = (MethodHeader)parts.pop ();
	body = (MethodBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + header + " " + body + "}";
    }

    public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	body.visit (visitor);
    }

    public String getMethodName () {
	return header.getMethodName ();
    }
}
