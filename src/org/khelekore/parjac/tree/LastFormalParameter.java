package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

/** Vararg parameter */
public class LastFormalParameter extends FlaggedTypeBase {
    private final TreeNode type;
    private final List<TreeNode> annotations;
    private final VariableDeclaratorId vdi;

    public LastFormalParameter (Rule r, Deque<TreeNode> parts, ParsePosition pos,
				Path path, CompilerDiagnosticCollector diagnostics) {
	super (parts.peek () instanceof ZOMEntry, parts, pos, path, diagnostics);
	type = parts.pop ();
	annotations = parts.peek () instanceof ZOMEntry ? ((ZOMEntry)parts.pop ()).get () : null;
	vdi = (VariableDeclaratorId)parts.pop ();
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos,
				  Path path, CompilerDiagnosticCollector diagnostics) {
	if (r.size () == 1)
	    return parts.pop ();
	return new LastFormalParameter (r, parts, pos, path, diagnostics);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getAnnotations () + " " + getFlags () +
	    " " + type + " " + annotations + " " + vdi + "}";
    }

    public TreeNode getType () {
	return type;
    }

    public String getId () {
	return vdi.getId ();
    }

    @Override public ExpressionType getExpressionType () {
	return ExpressionType.array (type.getExpressionType (), 1);
    }
}
