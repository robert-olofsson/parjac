package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class EnumConstant implements TreeNode {
    private final List<TreeNode> annotations;
    private final String id;
    private final ArgumentList arguments;
    private final ClassBody classBody;

    public EnumConstant (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
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
	if (r.size () > pos && r.getRulePart (pos).getId () == Token.LEFT_PARENTHESIS) {
	    pos++; // '('
	    if (r.getRulePart (pos).getId () != Token.RIGHT_PARENTHESIS) {
		arguments = (ArgumentList)parts.pop ();
		pos++;
	    } else {
		arguments = null;
	    }
	    pos++; // ')'
	} else {
	    arguments = null;
	}
	classBody = r.size () > pos ? (ClassBody)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations +
	    ", id: " + id + ", arguments: " + arguments + ", classBody: " + classBody + "}";
    }
}