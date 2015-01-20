package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.tree.ZOMEntry;

public class ClassBody implements TreeNode {
    private final List<TreeNode> declarations;

    public ClassBody (Rule r, Deque<TreeNode> parts) {
	if (r.size () > 2) {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    declarations = ze.get ();
	} else {
	    declarations = Collections.emptyList ();
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{declarations: " + declarations + "}";
    }
}
