package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class TypeBound extends PositionNode {
    private final ClassType ct;
    private final List<AdditionalBound> additionalBounds;

    public TypeBound (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	ct = (ClassType)parts.pop ();
	additionalBounds = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ct + " " + additionalBounds + "}";
    }

    @Override public ExpressionType getExpressionType () {
	return ct.getExpressionType ();
    }

    public ClassType getType () {
	return ct;
    }

    public List<AdditionalBound> getAdditionalBounds () {
	return additionalBounds;
    }

    public int size () {
	int ret = 1;
	if (additionalBounds != null)
	    ret += additionalBounds.size ();
	return ret;
    }
}
