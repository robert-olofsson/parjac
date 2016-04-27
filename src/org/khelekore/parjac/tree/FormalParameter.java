package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class FormalParameter extends FlaggedTypeBase {
    private final TreeNode type;
    private final VariableDeclaratorId vdi;

    public FormalParameter (Rule r, Deque<TreeNode> parts, ParsePosition pos,
			    Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.size () > 2, parts, pos, path, diagnostics);
	type = parts.pop ();
	vdi = (VariableDeclaratorId)parts.pop ();
    }

    public FormalParameter (TreeNode type, VariableDeclaratorId vdi) {
	super (null, null, null, null);
	this.type = type;
	this.vdi = vdi;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getAnnotations () + " " + getFlags () +
	    " " + type + " " + vdi + "}";
    }

    public TreeNode getType () {
	return type;
    }

    public String getId () {
	return vdi.getId ();
    }

    @Override public ExpressionType getExpressionType () {
	return type.getExpressionType ();
    }
}
