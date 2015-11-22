package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public abstract class Result extends PositionNode {
    private Result (ParsePosition pos) {
	super (pos);
    }

    public static final class VoidResult extends Result {
	public VoidResult (ParsePosition pos) {
	    super (pos);
	}

	@Override protected String getStringDesc () {
	    return "";
	}

	public TreeNode getReturnType () {
	    throw new IllegalStateException ("Void methods do not have a return type");
	}

	@Override public ExpressionType getExpressionType () {
	    return ExpressionType.VOID;
	}
    }

    public static class TypeResult extends Result {
	private final TreeNode type;

	public TypeResult (TreeNode type, ParsePosition pos) {
	    super (pos);
	    this.type = type;
	}

	@Override protected String getStringDesc () {
	    return type.toString ();
	}

	public TreeNode getReturnType () {
	    return type;
	}

	@Override public ExpressionType getExpressionType () {
	    return type.getExpressionType ();
	}
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.getRulePart (0).getId () == Token.VOID)
	    return new VoidResult (pos);
	return new TypeResult (parts.pop (), pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getStringDesc () + "}";
    }

    protected abstract String getStringDesc ();

    public abstract TreeNode getReturnType ();
}
