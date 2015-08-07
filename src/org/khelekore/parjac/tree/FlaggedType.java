package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.lexer.ParsePosition;

public abstract class FlaggedType extends PositionNode {
    private final List<TreeNode> annotations;
    private final int flags;

    public FlaggedType (boolean popModifiers, Deque<TreeNode> parts, ParsePosition pos,
			Path path, CompilerDiagnosticCollector diagnostics) {
	super (pos);
	List<TreeNode> modifiers = popModifiers ? ((ZOMEntry)parts.pop ()).get () : null;
	annotations = ModifierHelper.getAnnotations (modifiers);
	int mflags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
	flags = getFlags (mflags, modifiers);
    }

    public FlaggedType (List<TreeNode> modifiers, ParsePosition pos,
			Path path, CompilerDiagnosticCollector diagnostics) {
	super (pos);
	annotations = ModifierHelper.getAnnotations (modifiers);
	int mflags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
	flags = getFlags (mflags, modifiers);
    }

    public int getFlags (int flags, List<TreeNode> modifiers) {
	return flags;
    }

    public int getFlags () {
	return flags;
    }

    public List<TreeNode> getAnnotations () {
	return annotations;
    }
}