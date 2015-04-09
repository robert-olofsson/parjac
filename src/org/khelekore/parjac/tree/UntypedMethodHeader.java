package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class UntypedMethodHeader implements TreeNode {
    private final Result result;
    private final MethodDeclarator declarator;
    private final Throws thrown;

    public UntypedMethodHeader (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	result = (Result)parts.pop ();
	declarator = (MethodDeclarator)parts.pop ();
	thrown = r.size () > 2 ? (Throws)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + result + " " + declarator + " " + thrown + "}";
    }

    public Result getResult () {
	return result;
    }

    public String getMethodName () {
	return declarator.getId ();
    }

    public FormalParameterList getParameters () {
	return declarator.getParameters ();
    }
}
