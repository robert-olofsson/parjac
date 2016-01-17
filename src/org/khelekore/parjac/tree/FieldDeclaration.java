package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class FieldDeclaration extends VariableDeclaration {
    public FieldDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition pos,
			     Path path, CompilerDiagnosticCollector diagnostics) {
	super (r, 3, parts, pos, path, diagnostics);
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    getVariables ().visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (getVariables ());
    }
}
