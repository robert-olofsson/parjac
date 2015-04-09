package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class EnhancedForStatement implements TreeNode {
    private final List<TreeNode> modifiers;
    private final TreeNode type;
    private final VariableDeclaratorId vdi;
    private final TreeNode exp;
    private final TreeNode statement;

    public EnhancedForStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	modifiers = r.size () > 8 ? ((ZOMEntry)parts.pop ()).get () : null;
	type = parts.pop ();
	vdi = (VariableDeclaratorId)parts.pop ();
	parts.pop (); // ':'
	exp = parts.pop ();
	statement = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{for (" + modifiers + " " +
	    type + " " + vdi + " : " + exp + ") " + statement + "}";
    }
}
