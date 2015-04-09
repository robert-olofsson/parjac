package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public abstract class Result implements TreeNode {
    public static final VoidResult VOID_RESULT = new VoidResult ();

    public static final class VoidResult extends Result {
	@Override protected String getStringDesc () {
	    return "";
	}
    }

    public static class TypeResult extends Result {
	private final TreeNode type;

	public TypeResult (TreeNode type) {
	    this.type = type;
	}

	@Override protected String getStringDesc () {
	    return type.toString ();
	}

	public TreeNode get () {
	    return type;
	}
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.getRulePart (0).getId () == Token.VOID)
	    return VOID_RESULT;
	return new TypeResult (parts.pop ());
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getStringDesc () + "}";
    }

    protected abstract String getStringDesc ();
}
