package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class PrimaryNoNewArray {
    public static final ThisPrimary THIS_PRIMARY = new ThisPrimary ();
    public static final VoidClass VOID_CLASS = new VoidClass ();

    public static class ThisPrimary implements TreeNode {
	@Override public String toString () {
	    return getClass ().getSimpleName () + "{}";
	}
    }

    public static class DottedThis implements TreeNode {
	private final DottedName name;
	public DottedThis (DottedName name) {
	    this.name = name;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + name + ".this}";
	}
    }

    public static class VoidClass implements TreeNode {
	@Override public String toString () {
	    return getClass ().getSimpleName () + "{}";
	}
    }

    public static class ClassPrimary implements TreeNode {
	private final TreeNode type;
	private final List<Brackets> brackets;

	public ClassPrimary (TreeNode type, List<Brackets> brackets) {
	    this.type = type;
	    this.brackets = brackets;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + type + "  " + brackets + ".class}";
	}
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	if (r.getRulePart (0).getId () == Token.THIS)
	    return THIS_PRIMARY;
	if (r.size () == 1)
	    return parts.pop ();
	if (r.getRulePart (0).getId () == Token.LEFT_PARENTHESIS)
	    return parts.pop ();
	if (r.getRulePart (2).getId () == Token.THIS)
	    return new DottedThis ((DottedName)parts.pop ());
	if (r.getRulePart (0).getId () == Token.VOID)
	    return VOID_CLASS;
	return new ClassPrimary (parts.pop (), r.size () > 3 ? ((ZOMEntry)parts.pop ()).get () : null);
    }
}
