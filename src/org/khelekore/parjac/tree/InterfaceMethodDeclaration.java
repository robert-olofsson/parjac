package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class InterfaceMethodDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final MethodHeader header;
    private final MethodBody body;

    public InterfaceMethodDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	modifiers = r.size () > 2 ? ((ZOMEntry)parts.pop ()).get () : null;
	header = (MethodHeader)parts.pop ();
	body = (MethodBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + header + " " + body + "}";
    }
}
