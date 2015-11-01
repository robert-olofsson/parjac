package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ClassInstanceCreationExpression extends PositionNode {
    private TreeNode from;
    private final TypeArguments typeArguments;
    private final List<TreeNode> annotations;
    private final ClassType id;
    private final TreeNode typeArgumentsOrDiamond;
    private final ArgumentList args;
    private final ClassBody body;

    public ClassInstanceCreationExpression (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	if (r.size () > 1)
	    from = parts.pop ();
	UntypedClassInstanceCreationExpression u = (UntypedClassInstanceCreationExpression)parts.pop ();
	typeArguments = u.getTypeArguments ();
	annotations = u.getAnnotations ();
	id = u.getId ();
	typeArgumentsOrDiamond = u.getTypeArgumentsOrDiamond ();
	args = u.getArgumentList ();
	body = u.getBody ();
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	return new ClassInstanceCreationExpression (r, parts, pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + from + "." + "new " + typeArguments +
	    " " + annotations + " " + id + " " + typeArgumentsOrDiamond + "(" + args + ") " +
	    body + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (from != null) {
	    from.visit (visitor);
	}
	if (args != null)
	    args.visit (visitor);
	visitor.visit (this);
	if (body != null) {
	    if (visitor.anonymousClass (from, id, body))
		body.visit (visitor);
	    visitor.endAnonymousClass (id, body);
	}
    }

    public TreeNode getFrom () {
	return from;
    }

    public void setFrom (TreeNode from) {
	this.from = from;
    }

    public TypeArguments getTypeArguments () {
	return typeArguments;
    }

    public ClassType getId () {
	return id;
    }

    @Override public ExpressionType getExpressionType () {
	return new ExpressionType (id.getFullName ());
    }
}
