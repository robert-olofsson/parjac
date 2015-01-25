package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class AnnotationTypeDeclaration implements TreeNode {
    private final List<TreeNode> modifiers;
    private final String id;
    private final AnnotationTypeBody body;

    public AnnotationTypeDeclaration (Rule r, Deque<TreeNode> parts) {
	modifiers = r.size () > 4 ? ((ZOMEntry)parts.pop ()).get () : null;
	id = ((Identifier)parts.pop ()).get ();
	body = (AnnotationTypeBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + modifiers + " " + id + " " + body + "}";
    }
}
