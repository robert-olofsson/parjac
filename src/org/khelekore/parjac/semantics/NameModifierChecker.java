package org.khelekore.parjac.semantics;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.ConstructorDeclaration;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.MethodBody;
import org.khelekore.parjac.tree.MethodDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;

import static org.objectweb.asm.Opcodes.*;

public class NameModifierChecker implements TreeVisitor {
    private final CompiledTypesHolder cth;
    private final ClassResourceHolder crh;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private int level = 0;

    public NameModifierChecker (CompiledTypesHolder cth, ClassResourceHolder crh,
				SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	this.cth = cth;
	this.crh = crh;
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void check () {
	tree.getCompilationUnit ().visit (this);
    }

    @Override public boolean visit (NormalClassDeclaration c) {
	checkNameAndFlags (c.getId (), c, c.getAccessFlags ());
	level++;
	ClassType ct = c.getSuperClass ();
	if (ct != null) {
	    int flags;
	    String fqn = ct.getFullName ();
	    if (fqn != null) {
		// TODO: need to make sure I can merge cth and crh and get some interface type back
		// TODO: with appropriate methods on it.
		TreeNode tn;
		if ((tn = cth.getType (fqn)) != null) {
		    // TODO: this can probably fail
		    NormalClassDeclaration clz = (NormalClassDeclaration)tn;
		    flags = clz.getAccessFlags ();
		} else {
		    flags = crh.getClassModifiers (fqn);
		}
		if (isFinal (flags))
		    diagnostics.report (new SourceDiagnostics (tree.getOrigin (), ct.getParsePosition (),
							       "Can not extend final class"));
	    }
	}
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	checkNameAndFlags (e.getId (), e, e.getAccessFlags ());
	level++;
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	checkNameAndFlags (i.getId (), i, i.getAccessFlags ());
	level++;
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	checkNameAndFlags (a.getId (), a, a.getAccessFlags ());
	level++;
	return true;
    }

    @Override public void endType () {
	level--;
    }

    private void checkNameAndFlags (String id, TreeNode tn, int accessFlags) {
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

	if (isStatic (accessFlags) && level == 0)
	    diagnostics.report (new SourceDiagnostics (tree.getOrigin (), tn.getParsePosition (),
						       "Top level type may not be static"));
    }

    @Override public boolean visit (ConstructorDeclaration c) {
	checkAccess (c, c.getFlags ());
	return true;
    }

    @Override public void visit (FieldDeclaration f) {
	int flags = f.getFlags ();
	checkAccess (f, flags);
	if (isFinal (flags) && isVolatile (flags))
	    diagnostics.report (new SourceDiagnostics (tree.getOrigin (), f.getParsePosition (),
						       "Field may not be both final and volatile"));
    }

    @Override public boolean visit (MethodDeclaration m) {
	int flags = m.getFlags ();
	checkAccess (m, flags);
	if (isNative (flags) && isStrictFp (flags))
	    diagnostics.report (new SourceDiagnostics (tree.getOrigin (), m.getParsePosition (),
						       "method may not be both native and strictfp"));
	if (isAbstract (flags)) {
	    if  (isPrivate (flags) || isStatic (flags) || isFinal (flags) ||
		 isNative (flags) || isStrictFp (flags) || isSynchronized (flags)) {
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), m.getParsePosition (),
							   "Mixing abstract with non allowed flags"));
	    }
	    MethodBody body = m.getBody ();
	    if (body != MethodBody.EMPTY_BODY)
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), m.getParsePosition (),
							   "Abstract method may not have a body"));
	}

	if (isNative (flags)) {
	    MethodBody body = m.getBody ();
	    if (body != MethodBody.EMPTY_BODY)
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), m.getParsePosition (),
							   "Native method may not have a body"));
	}
	MethodBody body = m.getBody ();
	if (body == MethodBody.EMPTY_BODY && !(isAbstract (flags) || isNative (flags))) {
	    diagnostics.report (new SourceDiagnostics (tree.getOrigin (), m.getParsePosition (),
						       "Empty method body is only allowed for native " +
						       "and abstract methods"));
	}
	return true;
    }

    private void checkAccess (TreeNode tn, int flags) {
	int count = 0;
	if (isPublic (flags))
	    count++;
	if (isProtected (flags))
	    count++;
	if (isPrivate (flags))
	    count++;
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

    private boolean isFinal (int f) {
	return (f & ACC_FINAL) == ACC_FINAL;
    }

    private boolean isVolatile (int f) {
	return (f & ACC_VOLATILE) == ACC_VOLATILE;
    }

    private boolean isNative (int f) {
	return (f & ACC_NATIVE) == ACC_NATIVE;
    }

    private boolean isStrictFp (int f) {
	return (f & ACC_STRICT) == ACC_STRICT;
    }

    private boolean isAbstract (int f) {
	return (f & ACC_ABSTRACT) == ACC_ABSTRACT;
    }

    private boolean isStatic (int f) {
	return (f & ACC_STATIC) == ACC_STATIC;
    }

    private boolean isSynchronized (int f) {
	return (f & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED;
    }
}