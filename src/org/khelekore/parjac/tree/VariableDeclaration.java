package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

/** Base class for fields and method variables.
 */
public abstract class VariableDeclaration extends FlaggedType {
    private final TreeNode type;
    private final VariableDeclaratorList variables;

    public VariableDeclaration (Rule r, int modifierSize, Deque<TreeNode> parts, ParsePosition pos,
				Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.size () > modifierSize, parts, pos, path, diagnostics);
	type = parts.pop ();
	variables = (VariableDeclaratorList)parts.pop ();
    }

    public VariableDeclaration (List<TreeNode> modifiers, TreeNode type,
				VariableDeclaratorList variables, ParsePosition pos,
				Path path, CompilerDiagnosticCollector diagnostics) {
	super (modifiers, pos, path, diagnostics);
	this.type = type;
	this.variables = variables;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + getAnnotations () +
	    ", flags: " + getFlags () + ", type: " + type + ", variables: " + variables + "}";
    }

    public TreeNode getType () {
	return type;
    }

    public VariableDeclaratorList getVariables () {
	return variables;
    }

    @Override public ExpressionType getExpressionType () {
	return type.getExpressionType ();
    }
}
