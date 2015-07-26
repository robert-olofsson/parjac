package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class SwitchLabel extends PositionNode {
    private final TreeNode constantExp;

    public SwitchLabel (TreeNode constantExp, ParsePosition pos) {
	super (pos);
	this.constantExp = constantExp;
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.size () == 1)
	    return new DefaultLabel (pos);
	TreeNode constantExp = parts.pop ();
	parts.pop (); // ':'
	return new SwitchLabel (constantExp, pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + constantExp + "}";
    }
}
