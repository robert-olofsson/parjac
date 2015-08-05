package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class EnhancedForStatement extends PositionNode {
    private final LocalVariableDeclaration lvd;
    private final TreeNode exp;
    private final TreeNode statement;

    public EnhancedForStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos,
				 Path path, CompilerDiagnosticCollector diagnostics) {
	super (pos);
	List<TreeNode> modifiers = r.size () > 8 ? ((ZOMEntry)parts.pop ()).get () : null;
	TreeNode type = parts.pop ();
	VariableDeclaratorId vdi = (VariableDeclaratorId)parts.pop ();
	VariableDeclarator vd = new VariableDeclarator (vdi, null, pos);
	List<VariableDeclarator> ls = Collections.singletonList (vd);
	VariableDeclaratorList variables = new VariableDeclaratorList (ls, pos);
	lvd = new LocalVariableDeclaration (pos, modifiers, type, variables, path, diagnostics);
	parts.pop (); // ':'
	exp = parts.pop ();
	statement = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{for (" + lvd + " : " + exp + ") " + statement + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    visitor.visit (lvd);
	    if (exp != null)
		exp.visit (visitor);
	    statement.visit (visitor);
	}
    }

    public LocalVariableDeclaration getLocalVariableDeclaration () {
	return lvd;
    }

    public TreeNode getExpression () {
	return exp;
    }

    public TreeNode getStatement () {
	return statement;
    }
}
