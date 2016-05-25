package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class CatchType extends PositionNode {
    private List<ClassType> types;
    private ExpressionType et;

    public CatchType (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
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

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
    }

    @Override public ExpressionType getExpressionType () {
	return et;
    }

    public void setExpressionType (ExpressionType et) {
	this.et = et;
    }

    public List<ClassType> getTypes () {
	return types;
    }
}
