package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class CatchFormalParameter extends FlaggedTypeBase {
    private final CatchType type;
    private final VariableDeclaratorId vdi;

    public CatchFormalParameter (Rule r, Deque<TreeNode> parts, ParsePosition pos,
				 Path path, CompilerDiagnosticCollector diagnostics) {
	super (r.size () > 2, parts, pos, path, diagnostics);
	type = (CatchType)parts.pop ();
	vdi = (VariableDeclaratorId)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () +
	    "{" + getFlags () + " " +  getAnnotations () + " " + type + " " + vdi + "}";
    }

    public String getId () {
	return vdi.getId ();
    }

    @Override public void visit (TreeVisitor visitor) {
	type.visit (visitor);
	vdi.visit (visitor);
    }

    @Override public Collection<? extends TreeNode> getChildNodes () {
	return Arrays.asList (type, vdi);
    }

    @Override public ExpressionType getExpressionType () {
	return type.getExpressionType ();
    }

    public List<ClassType> getTypes () {
	return type.getTypes ();
    }
}
