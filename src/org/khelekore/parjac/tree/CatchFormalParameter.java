package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class CatchFormalParameter implements TreeNode {
    private final List<TreeNode> modifiers;
    private final CatchType type;
    private final VariableDeclaratorId vdi;

    public CatchFormalParameter (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	modifiers = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	type = (CatchType)parts.pop ();
	vdi = (VariableDeclaratorId)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + type + " " + vdi + "}";
    }
}
