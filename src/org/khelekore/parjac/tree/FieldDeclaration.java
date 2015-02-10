package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.tree.ZOMEntry;

public class FieldDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final TreeNode type;
    private final VariableDeclaratorList variables;

    public FieldDeclaration (Rule r, Deque<TreeNode> parts) {
	modifiers = r.size () > 3 ? ((ZOMEntry)parts.pop ()).get () : null;
	type = parts.pop ();
	variables = (VariableDeclaratorList)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{modifiers: " + modifiers +
	    ", type: " + type + ", variables: " + variables + "}";
    }

    public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	variables.visit (visitor);
    }

    public List<TreeNode> getModifiers () {
	return modifiers;
    }

    public VariableDeclaratorList getVariables () {
	return variables;
    }
}
