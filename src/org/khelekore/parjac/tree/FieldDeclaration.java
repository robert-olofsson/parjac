package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.ZOMEntry;

public class FieldDeclaration extends PositionNode {
    private final List<TreeNode> annotations;
    private final int flags;
    private final TreeNode type;
    private final VariableDeclaratorList variables;

    public FieldDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	List<TreeNode> modifiers = r.size () > 3 ? ((ZOMEntry)parts.pop ()).get () : null;
	annotations = ModifierHelper.getAnnotations (modifiers);
	flags = ModifierHelper.getModifiers (modifiers);
	type = parts.pop ();
	variables = (VariableDeclaratorList)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations + ", flags: " + flags +
	    ", type: " + type + ", variables: " + variables + "}";
    }

    public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	variables.visit (visitor);
    }

    public List<TreeNode> getAnotations () {
	return annotations;
    }

    public int getFlags () {
	return flags;
    }

    public TreeNode getType () {
	return type;
    }

    public VariableDeclaratorList getVariables () {
	return variables;
    }
}
