package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class LocalVariableDeclaration extends VariableDeclaration {
    public LocalVariableDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos,
				     Path path, CompilerDiagnosticCollector diagnostics) {
	super (r, 2, parts, pos, path, diagnostics);
    }

    public LocalVariableDeclaration (ParsePosition pos, List<TreeNode> modifiers,
				     TreeNode type, VariableDeclaratorList variables,
				     Path path, CompilerDiagnosticCollector diagnostics) {
	super (pos, modifiers, type, variables, path, diagnostics);
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	getVariables ().visit (visitor);
    }
}
