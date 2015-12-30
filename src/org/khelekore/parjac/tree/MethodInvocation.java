package org.khelekore.parjac.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.semantics.MethodInformation;
import org.objectweb.asm.Type;

public class MethodInvocation extends PositionNode {
    private TreeNode on;
    private final TypeArguments types;
    private final boolean isSuper;
    private final UntypedMethodInvocation mi;
    private MethodInformation actualMethod;
    private ExpressionType returnType;

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

    public MethodInvocation (UntypedMethodInvocation mi, ParsePosition ppos) {
	super (ppos);
	on = null;
	types = null;
	isSuper = false;
	this.mi = mi;
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 1)
	    return new MethodInvocation ((UntypedMethodInvocation)parts.pop (), pos);
	return new MethodInvocation (r, parts, pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + on + " " + isSuper + " " + types + " " + mi + "}";
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    if (on != null)
		on.visit (visitor);
	    mi.visit (visitor);
	}
    }

    @Override public void simpleVisit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	if (on != null)
	    return Arrays.asList (on, mi);
	return Collections.singleton (mi);
    }

    public TreeNode getOn () {
	return on;
    }

    public void setOn (TreeNode on) {
	this.on = on;
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

    public void setMethodInformation (MethodInformation actualMethod) {
	this.actualMethod = actualMethod;
	this.returnType = ExpressionType.get (Type.getReturnType (actualMethod.getDesc ()));
    }

    @Override public ExpressionType getExpressionType () {
	return returnType;
    }

    public String getDescription () {
	return actualMethod.getDesc ();
    }

    public int getActualMethodFlags () {
	return actualMethod.getAccess ();
    }
}
