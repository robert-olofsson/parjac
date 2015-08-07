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

import static org.khelekore.parjac.semantics.FlagsHelper.*;

/** Check filename and modifier flags.
 */
public class NameModifierChecker implements TreeVisitor {
    private final ClassInformationProvider cip;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private int level = 0;

    public NameModifierChecker (ClassInformationProvider cip, SyntaxTree tree,
				CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void check () {
	tree.getCompilationUnit ().visit (this);
    }

    @Override public boolean visit (NormalClassDeclaration c) {
	checkNameAndFlags (c.getId (), c, c.getFlags ());
	level++;
	ClassType ct = c.getSuperClass ();
	if (ct != null) {
	    String fqn = ct.getFullName ();
	    if (fqn != null) {
		int flags = cip.getFlags (fqn);
		if (isFinal (flags))
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), ct.getParsePosition (),
								 "Can not extend final class"));
		if (isInterface (flags))
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), ct.getParsePosition (),
								 "Can not extend interface"));
	    }
	}
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	checkNameAndFlags (e.getId (), e, e.getFlags ());
	level++;
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	checkNameAndFlags (i.getId (), i, i.getFlags ());
	level++;
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	checkNameAndFlags (a.getId (), a, a.getFlags ());
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
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.getParsePosition (),
							     "Public top level type: %s" +
							     " should be in a file named: '%s" +
							     ".java', not in '%s'",
							     id, id, tree.getOrigin ().getFileName ()));
	    count++;
	}
	if (isProtected (accessFlags)) {
	    if (level == 0)
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.getParsePosition (),
							     "Top level type may not be protected"));
	    count++;
	}
	if (isPrivate (accessFlags)) {
	    if (level == 0)
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.getParsePosition (),
							     "Top level type may not be private"));
	    count++;
	}
	if (count > 1)
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.getParsePosition (),
							 "Type has too many access flags"));

	if (isStatic (accessFlags) && level == 0)
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.getParsePosition (),
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
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), f.getParsePosition (),
							 "Field may not be both final and volatile"));
	// TODO: need to check static for inner classes
    }

    @Override public boolean visit (MethodDeclaration m) {
	int flags = m.getFlags ();
	checkAccess (m, flags);
	if (isNative (flags) && isStrictFp (flags))
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
							 "method may not be both native and strictfp"));
	if (isAbstract (flags)) {
	    if  (isPrivate (flags) || isStatic (flags) || isFinal (flags) ||
		 isNative (flags) || isStrictFp (flags) || isSynchronized (flags)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
							     "Mixing abstract with non allowed flags"));
	    }
	    MethodBody body = m.getBody ();
	    if (!body.isEmpty ())
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
							     "Abstract method may not have a body"));
	}

	if (isNative (flags)) {
	    MethodBody body = m.getBody ();
	    if (!body.isEmpty ())
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
							     "Native method may not have a body"));
	}
	MethodBody body = m.getBody ();
	if (body.isEmpty () && !(isAbstract (flags) || isNative (flags))) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
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
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.getParsePosition (),
							 "Type has too many access flags"));
    }
}