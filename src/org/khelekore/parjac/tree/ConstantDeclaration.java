package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class ConstantDeclaration extends PositionNode {
    private final List<TreeNode> modifiers;
    private final TreeNode type;
    private final VariableDeclaratorList vdis;

    public ConstantDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	modifiers = r.size () > 3 ? ((ZOMEntry)parts.pop ()).get () : null;
	type = parts.pop ();
	vdis = ((VariableDeclaratorList)parts.pop ());
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + type + " " + vdis + ";}";
    }
}
