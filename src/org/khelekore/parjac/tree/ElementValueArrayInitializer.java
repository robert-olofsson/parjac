package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ElementValueArrayInitializer extends PositionNode {
    private final ElementValueList ls;

    public ElementValueArrayInitializer (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	ls = (ElementValueList)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + ls + "}";
    }
}