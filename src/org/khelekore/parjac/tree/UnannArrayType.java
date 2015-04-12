package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class UnannArrayType extends ArrayType {
    public UnannArrayType (Deque<TreeNode> parts, ParsePosition pos) {
	super (parts, pos);
    }
}
