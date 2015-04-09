package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ArrayCreationExpression implements TreeNode {
    private final TreeNode type;
    private final DimExprs dimexprs;
    private final Dims dims;
    private final ArrayInitializer initializer;

    public ArrayCreationExpression (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	type = parts.pop ();
	if (r.getRulePart (2).getId ().equals ("DimExprs")) {
	    dimexprs = (DimExprs)parts.pop ();
	    dims = r.size () > 3 ? (Dims)parts.pop () : null;
	    initializer = null;
	} else {
	    dimexprs = null;
	    dims = (Dims)parts.pop ();
	    initializer = (ArrayInitializer)parts.pop ();
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{new " + type + dimexprs + dims + initializer + "}";
    }
}
