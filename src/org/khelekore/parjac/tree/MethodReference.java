package org.khelekore.parjac.tree;

import java.util.Deque;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public abstract class MethodReference extends PositionNode {
    private MethodReference (ParsePosition pos) {
	super (pos);
    }

    public static class ConstructorMethodReference extends MethodReference {
	private final TreeNode type;
	private final TypeArguments types;

	public ConstructorMethodReference (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	    super (pos);
	    type = parts.pop ();
	    types = r.size () > 3 ? (TypeArguments)parts.pop () : null;
	}

	@Override public String getStringDesc () {
	    return type + "::" + (types != null ? types : "") + "new";
	}
    }

    public static class NormalMethodReference extends MethodReference {
	private final TreeNode type;
	private final TypeArguments types;
	private final String id;

	public NormalMethodReference (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	    super (pos);
	    type = parts.pop ();
	    types = r.size () > 3 ? (TypeArguments)parts.pop () : null;
	    id = ((Identifier)parts.pop ()).get ();
	}

	@Override public String getStringDesc () {
	    return type + "::" + (types != null ? types : "") + id;
	}
    }

    public static class SuperMethodReference extends MethodReference {
	private TreeNode name;
	private final TypeArguments types;
	private final String id;

	public SuperMethodReference (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	    super (pos);
	    if (r.size () > 4) {
		name = parts.pop ();
		types = r.size () > 5 ? (TypeArguments)parts.pop () : null;
	    } else {
		name = null;
		types = r.size () > 3 ? (TypeArguments)parts.pop () : null;
	    }
	    id = ((Identifier)parts.pop ()).get ();
	}

	@Override public String getStringDesc () {
	    return (name != null ? name + "." : "") + "super::" + (types != null ? types : "") + id;
	}

	public TreeNode getName () {
	    return name;
	}

	public void setName (TreeNode name) {
	    this.name = name;
	}
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.getRulePart (r.size () - 1).getId () == Token.NEW)
	    return new ConstructorMethodReference (r, parts, pos);
	if (r.getRulePart (0).getId () == Token.SUPER || r.size () > 4)
	    return new SuperMethodReference (r, parts, pos);
	return new NormalMethodReference (r, parts, pos);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getStringDesc () + "}";
    }

    protected abstract String getStringDesc ();
}
