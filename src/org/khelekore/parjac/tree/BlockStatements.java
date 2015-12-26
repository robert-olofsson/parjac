package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class BlockStatements extends PositionNode {
    private final List<TreeNode> statements;

    public BlockStatements (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	TreeNode tn = parts.pop ();
	if (r.size () > 1) {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    statements = ze.get ();
	    statements.add (0, tn);
	} else {
	    statements = Collections.singletonList (tn);
	}
    }

    public List<TreeNode> get () {
	return statements;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + statements + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	statements.forEach (s -> s.visit (visitor));
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return statements;
    }
}
