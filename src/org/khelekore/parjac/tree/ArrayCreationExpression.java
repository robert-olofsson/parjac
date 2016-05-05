package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ArrayCreationExpression extends PositionNode {
    private final TreeNode type;
    private final DimExprs dimexprs;
    private final Dims dims;
    private final ArrayInitializer initializer;

    public ArrayCreationExpression (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	type = parts.pop ();
	if (r.getRulePart (2).getId ().equals ("DimExprs")) {
	    dimexprs = (DimExprs)parts.pop ();
	    dims = r.size () > 3 ? (Dims)parts.pop () : null;
	    initializer = null;
	} else {
	    dimexprs = null;
	    dims = (Dims)parts.pop ();
	    initializer = (ArrayInitializer)parts.pop ();
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{new " + type + dimexprs + dims + initializer + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    type.visit (visitor);
	    if (dimexprs != null)
		dimexprs.visit (visitor);
	}
    }

    public TreeNode getType () {
	return type;
    }

    public DimExprs getDimExprs () {
	return dimexprs;
    }

    /** Visit this node */
    @Override public void simpleVisit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public ExpressionType getExpressionType () {
	ExpressionType et = type.getExpressionType ();
	int numDims = 0;
	if (dimexprs != null)
	    numDims += dimexprs.get ().size ();
	if (dims != null)
	    numDims += dims.get ().size ();
	return ExpressionType.array (et, numDims);
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	List<TreeNode> ret = new ArrayList<> ();
	ret.add (type);
	if (dimexprs != null)
	    ret.add (dimexprs);
	if (dims != null)
	    ret.add (dims);
	if (initializer != null)
	    ret.add (initializer);
	return ret;
    }
}
