package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class EnumConstant implements TreeNode {
    private final List<TreeNode> annotations;
    private final String id;
    //private final ArgumentList arguments
    private final ClassBody classBody;

    public EnumConstant (Rule r, Deque<TreeNode> parts) {
	int pos = 0;
	TreeNode tn = parts.pop ();
	if (r.getRulePart (pos).getId () != Token.IDENTIFIER) {
	    annotations = ((ZOMEntry)tn).get ();
	    tn = parts.pop ();
	    pos++;
	} else {
	    annotations = null;
	}
	id = ((Identifier)tn).get ();
	pos++;
	if (r.size () > pos) {
	    if (r.getRulePart (pos).getId () == Token.LEFT_PARENTHESIS) {
		if (r.getRulePart (pos + 1).getId () != Token.LEFT_PARENTHESIS) {
		    parts.pop (); // TODO: handle arguments
		    pos += 3;
		} else {
		    pos += 2;
		}
	    }
	}
	classBody = r.size () > pos ? (ClassBody)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations +
	    ", id: " + id + ", classBody: " + classBody + "}";
    }
}