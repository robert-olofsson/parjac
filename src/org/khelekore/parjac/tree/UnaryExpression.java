package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class UnaryExpression implements TreeNode {
    private final Token conversion;
    private final TreeNode exp;

    public UnaryExpression (Token conversion, TreeNode exp) {
	this.conversion = conversion;
	this.exp = exp;
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts) {
	if (r.size () == 1)
	    return parts.pop ();
	Token conversion = ((OperatorTokenType)parts.pop ()).get ();
	TreeNode exp = parts.pop ();
	return new UnaryExpression (conversion, exp);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + conversion + " " + exp + "}";
    }
}
