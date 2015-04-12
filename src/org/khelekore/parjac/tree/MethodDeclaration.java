package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class MethodDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final MethodHeader header;
    private final MethodBody body;

    public MethodDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
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
	visitor.endMethod (this);
    }

    public TypeParameters getTypeParameters () {
	return header.getTypeParameters ();
    }

    public List<TreeNode> getModifiers () {
	return modifiers;
    }

    public Result getResult () {
	return header.getResult ();
    }

    public String getMethodName () {
	return header.getMethodName ();
    }

    public FormalParameterList getParameters () {
	return header.getParameters ();
    }
}
