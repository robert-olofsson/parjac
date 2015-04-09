package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class NormalAnnotation implements TreeNode  {
    private DottedName name;
    private ElementValuePairList pairs;

    public NormalAnnotation (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	name = (DottedName)parts.pop ();
	pairs = r.size () > 4 ? (ElementValuePairList)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + name + ", pairs: " + pairs + "}";
    }
}