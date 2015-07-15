package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

import static org.objectweb.asm.Opcodes.*;

public class InterfaceMethodDeclaration extends MethodDeclaration {
    public InterfaceMethodDeclaration (Rule r, Deque<TreeNode> parts, ParsePosition ppos,
				       Path path, CompilerDiagnosticCollector diagnostics) {
	super (r, parts, ppos, path, diagnostics);
    }

    @Override public int getFlags (int flags, List<TreeNode> modifiers) {
	boolean hasDefault = false;
	if (modifiers != null) {
	    for (TreeNode tn : modifiers) {
		ModifierTokenType mtt = (ModifierTokenType)tn;
		if (mtt.get () == Token.DEFAULT)
		    hasDefault = true;
	    }
	}

	flags |= ACC_PUBLIC;
	if (!hasDefault && !isStatic  (flags))
	    flags |= ACC_ABSTRACT;
	return flags;
    }

    private boolean isStatic (int f) {
	return (f & ACC_STATIC) > 0;
    }
}
