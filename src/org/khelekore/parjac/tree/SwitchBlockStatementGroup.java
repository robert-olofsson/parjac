package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class SwitchBlockStatementGroup extends PositionNode {
    private final List<SwitchLabel> labels;
    private final TreeNode blockStatements;

    public SwitchBlockStatementGroup (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	SwitchLabel l0 = (SwitchLabel)parts.pop ();
	if (r.size () > 2) {
	    labels = ((ZOMEntry)parts.pop ()).get ();
	    labels.add (0, l0);
	} else {
	    labels = Collections.singletonList (l0);
	}
	blockStatements = parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + labels + " " + blockStatements + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    labels.forEach (l -> l.visit (visitor));
	    blockStatements.visit (visitor);
	}
    }

    public Collection<? extends TreeNode> getChildNodes () {
	List<TreeNode> ls = new ArrayList<> (labels.size () + 1);
	ls.addAll (labels);
	ls.add (blockStatements);
	return ls;
    }

    public List<SwitchLabel> getLabels () {
	return labels;
    }

    public TreeNode getStatements () {
	return blockStatements;
    }
}
