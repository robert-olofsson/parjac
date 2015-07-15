package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;

public class AnnotationTypeDeclaration extends PositionNode {
    private final List<TreeNode> annotations;
    private final int accessFlags;
    private final String id;
    private final AnnotationTypeBody body;

    public AnnotationTypeDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
				      Path path, CompilerDiagnosticCollector diagnostics) {
	super (ppos);
	List<TreeNode> modifiers = r.size () > 4 ? ((ZOMEntry)parts.pop ()).get () : null;
	annotations = ModifierHelper.getAnnotations (modifiers);
	accessFlags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
	id = ((Identifier)parts.pop ()).get ();
	body = (AnnotationTypeBody)parts.pop ();
    }

    @Override public String toString () {
	return getClass ().getSimpleName () +
	    "{" + annotations + ", " + accessFlags + " " + id + " " + body + "}";
    }

    public int getAccessFlags () {
	return accessFlags;
    }

    public List<TreeNode> getAnntations () {
	return annotations;
    }

    public String getId () {
	return id;
    }

    public AnnotationTypeBody getBody () {
	return body;
    }

    public void visit (TreeVisitor visitor) {
	if (visitor.visit (this))
	    body.visit (visitor);
	visitor.endType ();
    }
}
