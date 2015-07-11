package org.khelekore.parjac.semantics;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;

import static org.objectweb.asm.Opcodes.*;

public class NameModifierChecker implements TreeVisitor {
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private int level = 0;

    public NameModifierChecker (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void check () {
	tree.getCompilationUnit ().visit (this);
    }

    @Override public boolean visit (NormalClassDeclaration c) {
	checkFileName (c.getId (), c, c.getAccessFlags ());
	level++;
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	checkFileName (e.getId (), e, e.getAccessFlags ());
	level++;
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	checkFileName (i.getId (), i, i.getAccessFlags ());
	level++;
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	checkFileName (a.getId (), a, a.getAccessFlags ());
	level++;
	return true;
    }

    @Override public void endType () {
	level--;
    }

    private void checkFileName (String id, TreeNode tn, int accessFlags) {
	int count = 0;
	if (isPublic (accessFlags)) {
	    if (level == 0 && tree.getOrigin () != null &&
		!(id + ".java").equals (tree.getOrigin ().getFileName ().toString ()))
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), tn.getParsePosition (),
							   "Public top level type: " + id +
							   " should be in a file named: " + id +
							   ".java, not in " + tree.getOrigin ().getFileName ()));
	    count++;
	}
	if (isProtected (accessFlags)) {
	    if (level == 0)
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), tn.getParsePosition (),
							   "Top level type may not be protected"));
	    count++;
	}
	if (isPrivate (accessFlags)) {
	    if (level == 0)
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), tn.getParsePosition (),
							   "Top level type may not be private"));
	    count++;
	}
	if (count > 1)
	    diagnostics.report (new SourceDiagnostics (tree.getOrigin (), tn.getParsePosition (),
						       "Type has too many access flags"));
    }

    private boolean isPublic (int accessFlags) {
	return (accessFlags & ACC_PUBLIC) == ACC_PUBLIC;
    }

    private boolean isProtected (int accessFlags) {
	return (accessFlags & ACC_PROTECTED) == ACC_PROTECTED;
    }

    private boolean isPrivate (int accessFlags) {
	return (accessFlags & ACC_PRIVATE) == ACC_PRIVATE;
    }
}