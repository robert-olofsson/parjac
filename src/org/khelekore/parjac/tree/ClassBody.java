package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ClassBody extends BodyBase {
    public ClassBody () {
    }

    public ClassBody (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (r, parts, pos);
    }
}
