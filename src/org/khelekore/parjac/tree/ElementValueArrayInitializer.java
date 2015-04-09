package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ElementValueArrayInitializer implements TreeNode  {
    private final ElementValueList ls;

    public ElementValueArrayInitializer (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	ls = (ElementValueList)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ls + "}";
    }
}