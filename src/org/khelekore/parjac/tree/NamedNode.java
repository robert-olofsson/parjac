package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class NamedNode implements TreeNode {
    private final DottedName name;

    public NamedNode (DottedName name, ParsePosition ppos) {
	this.name = name;
    }

    public NamedNode (Deque<TreeNode> parts, ParsePosition ppos) {
	this ((DottedName)parts.pop (), ppos);
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	NamedNode nn = (NamedNode)o;
	return name.equals (nn.name);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + name + "}";
    }

    public DottedName getName () {
	return name;
    }
}