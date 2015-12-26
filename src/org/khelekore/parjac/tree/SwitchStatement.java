package org.khelekore.parjac.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class SwitchStatement extends PositionNode {
    private final TreeNode exp;
    private final SwitchBlock block;

    public SwitchStatement (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (pos);
	exp = parts.pop ();
	block = (SwitchBlock)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{switch (" + exp + ")" + block + "}";
    }

    @Override public void visit (TreeVisitor visitor) {
	if (visitor.visit (this)) {
	    exp.visit (visitor);
	    block.visit (visitor);
	}
    }

    public Collection<? extends TreeNode> getChildNodes () {
	return Arrays.asList (exp, block);
    }

    public TreeNode getExpression () {
	return exp;
    }

    public SwitchBlock getBlock () {
	return block;
    }

}
