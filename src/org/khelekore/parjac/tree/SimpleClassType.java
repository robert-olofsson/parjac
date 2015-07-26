package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class SimpleClassType extends PositionNode {
    private final List<TreeNode> annotations;
    private final String id;
    private final TypeArguments typeArguments;

    public SimpleClassType (List<TreeNode> annotations,
			    String id,
			    TypeArguments typeArguments,
			    ParsePosition pos) {
	super (pos);
	this.annotations = annotations;
	this.id = id;
	this.typeArguments = typeArguments;
    }

    public SimpleClassType (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	int len = 1;
	TreeNode tn = parts.pop ();
	if (tn instanceof ZOMEntry) {
	    annotations = ((ZOMEntry)tn).get ();
	    tn = parts.pop ();
	    len++;
	} else {
	    annotations = null;
	}
	id = ((Identifier)tn).get ();
	typeArguments = r.size () > len ? (TypeArguments)parts.pop () : null;
    }

    public String getId () {
	return id;
    }

    public TypeArguments getTypeArguments () {
	return typeArguments;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + annotations + " " + id + " " + typeArguments + "}";
    }
}
