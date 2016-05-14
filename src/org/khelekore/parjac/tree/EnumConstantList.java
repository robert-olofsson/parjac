package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class EnumConstantList extends ListBase<EnumConstant> {

    public EnumConstantList (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	super (r, parts, pos);
    }

    @Override public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	super.visit (visitor);
	visitor.endEnumConstantList (this);
    }
}