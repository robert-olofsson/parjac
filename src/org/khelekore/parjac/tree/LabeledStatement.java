package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class LabeledStatement implements TreeNode {
    private final String id;
    private final TreeNode statement;

    public LabeledStatement (Rule r, Deque<TreeNode> parts) {
	id = ((Identifier)parts.pop ()).get ();
	parts.pop (); // ':'
	statement = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + id + " : " + statement + "}";
    }
}
