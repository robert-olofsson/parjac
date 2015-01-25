package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class CatchType implements TreeNode {
    private List<ClassType> types;

    public CatchType (Rule r, Deque<TreeNode> parts) {
	UnannClassType ct = (UnannClassType)parts.pop ();
	if (r.size () == 1) {
	    types = Collections.singletonList (ct);
	} else {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    types = new ArrayList<> (ze.size () + 1);
	    types.add (ct);
	    for (ExtraCatchType ect : ze.<ExtraCatchType>get ())
		types.add (ect.get ());
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + types + "}";
    }
}
