package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;

public class ElementValueArrayInitializer implements TreeNode  {
    private final ElementValueList ls;

    public ElementValueArrayInitializer (Rule r, Deque<TreeNode> parts) {
	ls = (ElementValueList)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ls + "}";
    }
}