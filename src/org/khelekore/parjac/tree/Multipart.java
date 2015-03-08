package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Multipart<T extends TreeNode> implements TreeNode {
    private List<T> data;

    public Multipart (T t) {
	data = Collections.singletonList (t);
    }

    public void add (T t) {
	if (data.size () == 1) {
	    T t0 = data.get (0);
	    data = new ArrayList<> ();
	    data.add (t0);
	}
	data.add (t);
    }

    public List<T> get () {
	return data;
    }

    public int size () {
	return data.size ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + data + "}";
    }
}