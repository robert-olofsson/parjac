package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class EnumBodyDeclarations extends PositionNode {
    private final List<TreeNode> classBodyDeclarations;

    public EnumBodyDeclarations (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	// we will add fields and methods so always initialize it
	classBodyDeclarations = r.size () > 1 ? ((ZOMEntry)parts.pop ()).get () : new ArrayList<> ();
    }

    public EnumBodyDeclarations (ParsePosition pos) {
	super (pos);
	classBodyDeclarations = new ArrayList<> ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + classBodyDeclarations + "}";
    }

    public List<TreeNode> getDeclarations () {
	return classBodyDeclarations;
    }

    public void visit (TreeVisitor visitor) {
	if (classBodyDeclarations != null)
	    classBodyDeclarations.forEach (d -> d.visit (visitor));
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return classBodyDeclarations != null ? classBodyDeclarations : Collections.emptyList ();
    }
}