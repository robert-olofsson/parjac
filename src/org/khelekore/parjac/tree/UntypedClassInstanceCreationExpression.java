package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public class UntypedClassInstanceCreationExpression implements TreeNode {
    private final TypeArguments typeArguments;
    private final List<TreeNode> annotations;
    private final ClassInstanceName id;
    private final TreeNode typeArgumentsOrDiamond;
    private final ArgumentList args;
    private final ClassBody body;

    public UntypedClassInstanceCreationExpression (Rule r, Deque<TreeNode> parts) {
	int pos = 1;
	if (r.getRulePart (pos).getId ().equals ("TypeArguments")) {
	    typeArguments = (TypeArguments)parts.pop ();
	    pos++;
	} else {
	    typeArguments = null;
	}
	if (!r.getRulePart (pos).getId ().equals ("ClassInstanceName")) {
	    annotations = ((ZOMEntry)parts.pop ()).get ();
	    pos++;
	} else {
	    annotations = null;
	}
	id = (ClassInstanceName)parts.pop ();
	pos++;
	if (r.getRulePart (pos).getId () != Token.LEFT_PARENTHESIS) {
	    typeArgumentsOrDiamond = parts.pop ();
	    pos++;
	} else {
	    typeArgumentsOrDiamond = null;
	}
	pos++; // '('
	if (r.getRulePart (pos).getId () != Token.RIGHT_PARENTHESIS) {
	    args = (ArgumentList)parts.pop ();
	    pos++;
	} else {
	    args = null;
	}
	pos++; // ')'
	body = r.size () > pos ? (ClassBody)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{new " + typeArguments + " " + annotations + " " +
	    id + " " + typeArgumentsOrDiamond + "(" + args + ") " + body + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (args != null)
	    args.visit (visitor);
	if (body != null) {
	    visitor.anonymousClass (body);
	    body.visit (visitor);
	    visitor.endType ();
	}
    }
}
