package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class LocalVariableDeclarationStatement extends PositionNode {
    private final LocalVariableDeclaration lvd;

    public LocalVariableDeclarationStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	lvd = (LocalVariableDeclaration)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + lvd + ";}";
    }

    @Override public void visit (TreeVisitor visitor) {
	lvd.visit (visitor);
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Collections.singleton (lvd);
    }
}
