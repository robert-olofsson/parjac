package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public abstract class MethodBody extends PositionNode {
    private MethodBody (ParsePosition pos) {
	super (pos);
    }

    public static class EmptyBody extends MethodBody {
	private EmptyBody (ParsePosition pos) {
	    super (pos);
	}

	@Override public String getStringDesc () {
	    return "";
	}

	@Override public boolean isEmpty () {
	    return true;
	}
    }

    public static class BlockBody extends MethodBody {
	private final Block block;

	public BlockBody (Block block, ParsePosition pos) {
	    super (pos);
	    this.block = block;
	}

	@Override public String getStringDesc () {
	    return block.toString ();
	}

	@Override public boolean isEmpty () {
	    return false;
	}

	public void visit (TreeVisitor visitor) {
	    block.visit (visitor);
	}
    }

    public static MethodBody build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.getRulePart (0).getId () == Token.SEMICOLON)
	    return new EmptyBody (pos);;
	return new BlockBody ((Block)parts.pop (), pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getStringDesc () + "}";
    }

    protected abstract String getStringDesc ();

    public abstract boolean isEmpty ();
}
