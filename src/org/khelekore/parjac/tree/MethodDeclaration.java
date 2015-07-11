package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class MethodDeclaration extends PositionNode {
    private final List<TreeNode> annotations;
    private final int flags;
    private final MethodHeader header;
    private final MethodBody body;

    public MethodDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	List<TreeNode> modifiers = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	annotations = ModifierHelper.getAnnotations (modifiers);
	flags = ModifierHelper.getModifiers (modifiers);
	header = (MethodHeader)parts.pop ();
	body = (MethodBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" +
	    annotations + " " + flags + " " + " " + header + " " + body + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endMethod (this);
    }

    public TypeParameters getTypeParameters () {
	return header.getTypeParameters ();
    }

    public List<TreeNode> getAnnotations () {
	return annotations;
    }

    public int getFlags () {
	return flags;
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
