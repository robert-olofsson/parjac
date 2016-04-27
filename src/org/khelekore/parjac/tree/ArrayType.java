package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.lexer.ParsePosition;

public class ArrayType extends PositionNode {
    private final TreeNode type;
    private final Dims dims;

    public ArrayType (Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	type = parts.pop ();
	dims = (Dims)parts.pop ();
    }

    public ArrayType (TreeNode type, Dims dims) {
	super (null);
	this.type = type;
	this.dims = dims;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + type + " " + dims + "}";
    }

    public TreeNode getType () {
	return type;
    }

    public Dims getDims () {
	return dims;
    }

    public int getDimensions () {
	return dims.size ();
    }

    @Override public ExpressionType getExpressionType () {
	ExpressionType base = type.getExpressionType ();
	return ExpressionType.array (base, getDimensions ());
    }
}
