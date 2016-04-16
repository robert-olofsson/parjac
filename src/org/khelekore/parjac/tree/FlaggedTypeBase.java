package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.lexer.ParsePosition;
import org.objectweb.asm.Opcodes;

public abstract class FlaggedTypeBase extends PositionNode implements FlaggedType {
    private final List<TreeNode> annotations;
    private int flags;

    public FlaggedTypeBase (boolean popModifiers, Deque<TreeNode> parts, ParsePosition pos,
			    Path path, CompilerDiagnosticCollector diagnostics) {
	super (pos);
	List<TreeNode> modifiers = popModifiers ? ((ZOMEntry)parts.pop ()).get () : null;
	annotations = ModifierHelper.getAnnotations (modifiers);
	int mflags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
	flags = getFlags (mflags, modifiers);
    }

    public FlaggedTypeBase (List<TreeNode> modifiers, ParsePosition pos,
			Path path, CompilerDiagnosticCollector diagnostics) {
	super (pos);
	annotations = ModifierHelper.getAnnotations (modifiers);
	int mflags = ModifierHelper.getModifiers (modifiers, path, diagnostics);
	flags = getFlags (mflags, modifiers);
    }

    public int getFlags (int flags, List<TreeNode> modifiers) {
	return flags;
    }

    @Override public int getFlags () {
	return flags;
    }

    public void makePublic () {
	flags |= Opcodes.ACC_PUBLIC;
    }

    public void setVarArgs () {
	flags |= Opcodes.ACC_VARARGS;
    }

    @Override public List<TreeNode> getAnnotations () {
	return annotations;
    }
}