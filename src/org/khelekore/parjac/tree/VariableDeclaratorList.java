package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.tree.VariableDeclarator;
import org.khelekore.parjac.tree.ZOMEntry;

public class VariableDeclaratorList implements TreeNode {
    private final List<VariableDeclarator> vds;

    public VariableDeclaratorList (Rule r, Deque<TreeNode> parts) {
	VariableDeclarator vd = (VariableDeclarator)parts.pop ();
	if (r.size () == 1) {
	    vds = Collections.singletonList (vd);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    vds = new ArrayList<> (1 + ze.size ());
	    vds.add (vd);
	    for (TreeNode tn : ze.get ())
		vds.add ((VariableDeclarator)tn);
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{vds: " + vds + "}";
    }
}