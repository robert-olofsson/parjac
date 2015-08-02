package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public abstract class SwitchLabel extends PositionNode {

    private SwitchLabel (ParsePosition pos) {
	super (pos);
    }

    public static class CaseLabel extends SwitchLabel {
	private final TreeNode constantExp;

	public CaseLabel (TreeNode constantExp, ParsePosition pos) {
	    super (pos);
	    this.constantExp = constantExp;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + constantExp + "}";
	}
    }

    public static class DefaultLabel extends SwitchLabel {
	public DefaultLabel (ParsePosition pos) {
	    super (pos);
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{}";
	}
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 2) {
	    parts.pop (); // 'default', will actually be a ModifierTokenType.
	    parts.pop (); // ':'
	    return new DefaultLabel (pos);
	}
	TreeNode constantExp = parts.pop ();
	parts.pop (); // ':'
	return new CaseLabel (constantExp, pos);
    }
}
