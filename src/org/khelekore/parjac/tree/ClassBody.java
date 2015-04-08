package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ClassBody extends BodyBase {
    public ClassBody () {
    }

    public ClassBody (Rule r, Deque<TreeNode> parts) {
	super (r, parts);
    }
}
