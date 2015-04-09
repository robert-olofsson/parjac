package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class SwitchBlockStatementGroup implements TreeNode {
    private final List<SwitchLabel> labels;
    private final TreeNode blockStatements;

    public SwitchBlockStatementGroup (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
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
}
