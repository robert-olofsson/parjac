package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.semantics.MethodInformation;

public class ClassInstanceCreationExpression extends PositionNode implements MethodInformationHolder {
    private final TreeNode from;
    private final TypeArguments typeArguments;
    private final List<TreeNode> annotations;
    private final ClassType id;
    private final TreeNode typeArgumentsOrDiamond;
    private final ArgumentList args;
    private final ClassBody body;
    private MethodInformation actualMethod;

    public ClassInstanceCreationExpression (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	from = r.size () > 1 ? parts.pop () : null;
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
	if (visitor.visit (this)) {
	    if (from != null) {
		from.visit (visitor);
	    }
	    if (args != null)
		args.visit (visitor);
	    if (body != null) {
		if (visitor.anonymousClass (from, id, body))
		    body.visit (visitor);
		visitor.endAnonymousClass (id, body);
	    }
	}
    }

    @Override public void simpleVisit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	List<TreeNode> ls = new ArrayList<> ();
	if (from != null)
	    ls.add (from);
	if (args != null)
	    ls.add (args);
	if (body != null)
	    ls.add (body);
	return ls;
    }

    public TreeNode getFrom () {
	return from;
    }

    public TypeArguments getTypeArguments () {
	return typeArguments;
    }

    public ClassType getId () {
	return id;
    }

    public ArgumentList getArgumentList () {
	return args;
    }

    public boolean hasBody ()  {
	return body != null;
    }

    public ClassBody getBody () {
	return body;
    }

    @Override public ExpressionType getExpressionType () {
	return new ExpressionType (id.getFullName ());
    }

    public void setMethodInformation (MethodInformation actualMethod) {
	this.actualMethod = actualMethod;
    }

    public String getDescription () {
	return actualMethod.getDesc ();
    }

    public int getActualMethodFlags () {
	return actualMethod.getAccess ();
    }
}
