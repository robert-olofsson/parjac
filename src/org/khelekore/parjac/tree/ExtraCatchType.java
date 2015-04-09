package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class ExtraCatchType implements TreeNode {
    private final ClassType ct;

    public ExtraCatchType (Deque<TreeNode> parts, ParsePosition ppos) {
	parts.pop (); // '|'
	ct = (ClassType)parts.pop ();
    }

    public ClassType get () {
	return ct;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ct + "}";
    }
}
