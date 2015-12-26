package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class FieldAccess extends PositionNode {
    private final TreeNode from;
    private final boolean isSuper;
    private final String id;
    private ExpressionType returnType;

    public FieldAccess (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
	int pos = 0;
	if (r.getRulePart (0).isRulePart ()) {
	    from = parts.pop ();
	    pos++;
	} else {
	    from = null;
	}
	isSuper = r.getRulePart (pos).getId () == Token.SUPER;
	id = ((Identifier)parts.pop ()).get ();
    }

    public FieldAccess (TreeNode from, String id, ParsePosition pos) {
	super (pos);
	this.from = from;
	isSuper = false;
	this.id = id;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + from + " " + isSuper + " " + id + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    if (from != null)
		from.visit (visitor);
	}
    }

    @Override public void simpleVisit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	if (from != null)
	    return Collections.singleton (from);
	return Collections.emptyList ();
    }

    public TreeNode getFrom () {
	return from;
    }

    public boolean isSuper () {
	return isSuper;
    }

    public String getFieldId () {
	return id;
    }

    public void setReturnType (ExpressionType returnType) {
	this.returnType = returnType;
    }

    @Override public ExpressionType getExpressionType () {
	return returnType;
    }
}
