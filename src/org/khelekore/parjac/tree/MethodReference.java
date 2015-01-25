package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Token;

public abstract class MethodReference implements TreeNode {

    private static class ConstructorMethodReference extends MethodReference {
	private final TreeNode type;
	private final TypeArguments types;

	public ConstructorMethodReference (Rule r, Deque<TreeNode> parts) {
	    type = parts.pop ();
	    types = r.size () > 3 ? (TypeArguments)parts.pop () : null;
	}

	@Override public String getStringDesc () {
	    return type + "::" + (types != null ? types : "") + "new";
	}
    }

    private static class NormalMethodReference extends MethodReference {
	private final TreeNode type;
	private final TypeArguments types;
	private final String id;

	public NormalMethodReference (Rule r, Deque<TreeNode> parts) {
	    type = parts.pop ();
	    types = r.size () > 3 ? (TypeArguments)parts.pop () : null;
	    id = ((Identifier)parts.pop ()).get ();
	}

	@Override public String getStringDesc () {
	    return type + "::" + (types != null ? types : "") + id;
	}
    }

    private static class SuperMethodReference extends MethodReference {
	private final DottedName name;
	private final TypeArguments types;
	private final String id;

	public SuperMethodReference (Rule r, Deque<TreeNode> parts) {
	    if (r.size () > 5) {
		name = (DottedName)parts.pop ();
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
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts) {
	if (r.getRulePart (r.size () - 1).getId () == Token.NEW)
	    return new ConstructorMethodReference (r, parts);
	if (r.getRulePart (0).getId () == Token.SUPER || r.size () > 5)
	    return new SuperMethodReference (r, parts);
	return new NormalMethodReference (r, parts);
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + getStringDesc () + "}";
    }

    protected abstract String getStringDesc ();
}
