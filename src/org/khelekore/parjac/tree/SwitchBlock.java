package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class SwitchBlock extends PositionNode {
    private final List<SwitchBlockStatementGroup> groups;
    private final List<SwitchLabel> labels;

    public SwitchBlock (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	List<SwitchBlockStatementGroup> groups = null;
	List<SwitchLabel> labels = null;

	if (r.size () > 2) {
	    ZOMEntry ze = (ZOMEntry)parts.pop ();
	    if (ze.getType ().equals ("SwitchBlockStatementGroup")) {
		groups = ze.get ();
		ze = r.size () > 3 ? (ZOMEntry)parts.pop () : null;
	    }
	    if (ze != null && ze.getType ().equals ("SwitchLabel"))
		labels = (ze).get ();
	}

	this.groups = groups;
	this.labels = labels;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + groups + " " + labels + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    if (groups != null)
		groups.forEach (s -> s.visit (visitor));
	}
	visitor.endSwitchBlock ();
    }

    public List<SwitchBlockStatementGroup> getGroups () {
	return groups;
    }

    public List<SwitchLabel> getLabels () {
	return labels;
    }
}
