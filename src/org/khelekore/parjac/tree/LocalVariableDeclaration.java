package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class LocalVariableDeclaration extends PositionNode {
    private final List<TreeNode> modifiers;
    private final TreeNode type;
    private final VariableDeclaratorList vds;

    public LocalVariableDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	modifiers = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	type = parts.pop ();
	vds = (VariableDeclaratorList)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + type + " " + vds + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	vds.visit (visitor);
    }
}
