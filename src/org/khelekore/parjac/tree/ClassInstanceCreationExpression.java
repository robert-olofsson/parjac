package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class ClassInstanceCreationExpression implements TreeNode {
    private final TreeNode from;
    private final UntypedClassInstanceCreationExpression ucice;

    public ClassInstanceCreationExpression (Rule r, Deque<TreeNode> parts) {
	from = parts.pop ();
	ucice = (UntypedClassInstanceCreationExpression)parts.pop ();
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    return parts.pop ();
	return new ClassInstanceCreationExpression (r, parts);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + from + "." + ucice + "}";
    }
}
