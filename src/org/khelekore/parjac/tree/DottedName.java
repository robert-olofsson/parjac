package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.List;

public class DottedName implements TreeNode {
    List<String> parts = new ArrayList<> ();

    public DottedName () {
    }

    public void add (String identifier) {
	parts.add (identifier);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + parts + "}";
    }
}