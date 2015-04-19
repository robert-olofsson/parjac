package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class WildcardBounds extends PositionNode {
    private final Token mode;
    private final ClassType refType;

    public WildcardBounds (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	mode = (Token)r.getRulePart (0).getId ();
	refType = (ClassType)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + mode + " " + refType + "}";
    }

    public ClassType getClassType () {
	return refType;
    }
}
