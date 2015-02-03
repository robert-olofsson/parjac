package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public abstract class MethodBody implements TreeNode {
    public static final EmptyBody EMPTY_BODY = new EmptyBody ();

    public static class EmptyBody extends MethodBody {
	@Override public String getStringDesc () {
	    return "";
	}
    }

    public static class BlockBody extends MethodBody {
	private final Block block;

	public BlockBody (Block block) {
	    this.block = block;
	}

	@Override public String getStringDesc () {
	    return block.toString ();
	}

	public void visit (TreeVisitor visitor) {
	    block.visit (visitor);
	}
    }

    public static MethodBody build (Rule r, Deque<TreeNode> parts) {
	if (r.getRulePart (0).getId () == Token.SEMICOLON)
	    return EMPTY_BODY;
	return new BlockBody ((Block)parts.pop ());
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getStringDesc () + "}";
    }

    protected abstract String getStringDesc ();
}
