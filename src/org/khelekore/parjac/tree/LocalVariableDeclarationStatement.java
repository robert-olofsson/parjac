package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class LocalVariableDeclarationStatement implements TreeNode {
    private final LocalVariableDeclaration lvd;

    public LocalVariableDeclarationStatement (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	lvd = (LocalVariableDeclaration)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + lvd + ";}";
    }

    @Override public void visit (TreeVisitor visitor) {
	lvd.visit (visitor);
    }
}
