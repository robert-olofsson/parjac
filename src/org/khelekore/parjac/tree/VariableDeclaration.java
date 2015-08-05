package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.ZOMEntry;

/** Base class for fields and method variables.
 */
public abstract class VariableDeclaration extends PositionNode {
    private final List<TreeNode> annotations;
    private final int flags;
    private final TreeNode type;
    private final VariableDeclaratorList variables;

    public VariableDeclaration (Rule r, int modifierSize, Deque<TreeNode> parts, ParsePosition pos,
				Path path, CompilerDiagnosticCollector diagnostics) {
	super (pos);
	List<TreeNode> modifiers = r.size () > modifierSize ? ((ZOMEntry)parts.pop ()).get () : null;
	annotations = ModifierHelper.getAnnotations (modifiers);
	flags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
	type = parts.pop ();
	variables = (VariableDeclaratorList)parts.pop ();
    }

    public VariableDeclaration (ParsePosition pos, List<TreeNode> modifiers,
				TreeNode type, VariableDeclaratorList variables,
				Path path, CompilerDiagnosticCollector diagnostics) {
	super (pos);
	annotations = ModifierHelper.getAnnotations (modifiers);
	flags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
	this.type = type;
	this.variables = variables;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations + ", flags: " + flags +
	    ", type: " + type + ", variables: " + variables + "}";
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
