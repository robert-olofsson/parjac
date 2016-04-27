package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.semantics.MethodInformation;
import org.objectweb.asm.Type;

public class ExplicitConstructorInvocation extends PositionNode implements MethodInformationHolder {
    private final TreeNode type;
    private final TypeArguments types;
    private final Token where;
    private final ConstructorArguments args;
    private MethodInformation actualMethod;

    public ExplicitConstructorInvocation (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
	int pos = 0;
	if (r.size () > 3) {
	    type = parts.pop ();
	    pos += 2;
	} else {
	    type = null;
	}
	if (r.getRulePart (pos).isRulePart ()) {
	    types = (TypeArguments)parts.pop ();
	    pos++;
	} else {
	    types = null;
	}
	where = (Token)r.getRulePart (pos).getId ();
	args = (ConstructorArguments)parts.pop ();
    }

    public ExplicitConstructorInvocation (TypeArguments types, Token where, ConstructorArguments args) {
	super (null);
	this.type = null;
	this.types = types;
	this.where = where;
	this.args = args;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + types + " " + where + " " + args + "}";
    }

    public TreeNode getType () {
	return type;
    }

    @Override public void visit (TreeVisitor visitor) {
	if (type != null)
	    type.visit (visitor);
	if (visitor.visit (this))
	    args.visit (visitor);
	visitor.endExplicitConstructorInvocation (this);
    }

    @Override public void simpleVisit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (args);
    }

    public ConstructorArguments getArguments () {
	return args;
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

    public Type[] getActualArgumentTypes () {
	return actualMethod.getArguments ();
    }
}
