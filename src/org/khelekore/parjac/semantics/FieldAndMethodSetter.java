package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.ArgumentList;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.FieldAccess;
import org.khelekore.parjac.tree.MethodInvocation;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;
import org.khelekore.parjac.tree.TreeWalker;
import org.objectweb.asm.Type;

public class FieldAndMethodSetter implements TreeVisitor {
    private final ClassInformationProvider cip;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final Deque<TreeNode> containingClasses = new ArrayDeque<> ();

    public FieldAndMethodSetter (ClassInformationProvider cip, SyntaxTree tree,
				 CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void run () {
	TreeWalker.walkBottomUp (this, tree.getCompilationUnit ());
    }

    @Override public boolean visit (NormalClassDeclaration c) {
	containingClasses.addLast (c);
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	containingClasses.addLast (e);
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	containingClasses.addLast (i);
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	containingClasses.addLast (a);
	return true;
    }

    @Override public void endType () {
	containingClasses.removeLast ();
    }

    @Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	containingClasses.addLast (ct);
	return true;
    }

    @Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	containingClasses.removeLast ();
    }

    @Override public boolean visit (MethodInvocation m) {
	TreeNode on = m.getOn ();
	ExpressionType clzType;
	if (on != null) {
	    clzType = on.getExpressionType ();
	} else {
	    String fqn = cip.getFullName (containingClasses.getLast ());
	    clzType = new ExpressionType (fqn);
	}
	if (clzType.isPrimitiveType ()) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 m.getParsePosition (),
							 "Primitives have no methods"));
	} else if (clzType == ExpressionType.VOID) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 m.getParsePosition (),
							 "Void has no methods"));
	} else if (clzType == ExpressionType.NULL) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 m.getParsePosition (),
							 "null has no methods"));
	} else {
	    String fqn = clzType.getClassName ();
	    String name = m.getId ();
	    ArgumentList al = m.getArgumentList ();
	    try {
		MethodInformation mi = findMatching (fqn, name, al);
		if (mi == null) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
								 m.getParsePosition (),
								 "No matching method found"));
		} else {
		    m.setDescription (mi.getDesc ());
		}
	    } catch (IOException e) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
								 m.getParsePosition (),
								 "Failed to read class: " + e));
	    }
	}
	return false;
    }

    private MethodInformation findMatching (String fqn, String name, ArgumentList al) throws IOException {
	Deque<String> typesToCheck = new ArrayDeque<> ();
	typesToCheck.addLast (fqn);
	Set<String> visitedTypes = new HashSet<> ();
	while (!typesToCheck.isEmpty ()) {
	    String clz = typesToCheck.removeFirst ();
	    if (visitedTypes.contains (clz))
		continue;
	    visitedTypes.add (clz);
	    Map<String, List<MethodInformation>> methods = cip.getMethods (clz);
	    if (methods != null) {
		List<MethodInformation> ls = methods.get (name);
		if (ls != null) {
		    for (MethodInformation mi : ls) {
			if (match (al, mi))
			    return mi;
		    }
		}
	    }
	    // not found in current class, try super class
	    Optional<List<String>> o = cip.getSuperTypes (clz);
	    if (o.isPresent ())
		typesToCheck.addAll (o.get ());
	}
	return null;
    }

    private boolean match (ArgumentList al, MethodInformation mi) {
	Type[] arguments = mi.getArguments ();
	if ((al == null || al.size () == 0) && arguments.length == 0)
	    return true;
	if (al.size () != arguments.length)
	    return false;
	int i = 0;
	for (TreeNode tn : al.get ()) {
	    ExpressionType et = tn.getExpressionType ();
	    if (!et.match (arguments[i++]))
		return false;
	}
	return true;
    }

    @Override public boolean visit (FieldAccess f) {
	f.setReturnType (cip.getFieldType (f.getFrom ().getExpressionType ().getClassName (),
					   f.getFieldId ()));
	return true;
    }
}