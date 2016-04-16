package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.ArgumentList;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassInstanceCreationExpression;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.FieldAccess;
import org.khelekore.parjac.tree.MethodInvocation;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TernaryExpression;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;
import org.khelekore.parjac.tree.TreeWalker;
import org.objectweb.asm.Opcodes;
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

    @Override public boolean visit (TernaryExpression t) {
	TreeNode tp = t.getThenPart ();
	TreeNode ep = t.getElsePart ();
	t.setExpressionType (lub (t, tp.getExpressionType (), ep.getExpressionType ()));
	return true;
    }

    private ExpressionType lub (TreeNode tn, ExpressionType et1, ExpressionType et2) {
	if (et1.isPrimitiveType ()) {
	    if (!et2.isPrimitiveType ()) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     tn.getParsePosition (),
							     "Unable to find common parent type of %s and %s",
							     et1, et2));
		return null;
	    }
	    return ExpressionType.bigger (et1, et2);
	}
	return et1;
    }

    @Override public boolean visit (ClassInstanceCreationExpression c) {
	ExpressionType clzType;
	if (c.hasBody ()) {
	    clzType = new ExpressionType (cip.getFullName (c.getBody ()));
	} else {
	    clzType = c.getId ().getExpressionType ();
	}
	if (hasMethods (clzType, c)) {
	    String fqn = clzType.getClassName ();
	    ArgumentList al = c.getArgumentList ();
	    try {
		List<MethodInformation> mis = findMatchingConstructor (fqn, al);
		if (mis == null) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), c.getParsePosition (),
								 "No matching constructor found for %s(%s)",
								 fqn, argList (al)));
		} else if (mis.size () > 1) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), c.getParsePosition (),
								 "Multiple matching constructor found for %s(%s)",
								 fqn, argList (al)));
		} else {
		    c.setMethodInformation (mis.get (0));
		}
	    } catch (IOException e) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     c.getParsePosition (),
							     "Failed to read class: " + e));
	    }
	}
	return true;
    }

    @Override public boolean visit (MethodInvocation m) {
	ExpressionType clzType = getOnType (m.getOn ());
	if (hasMethods (clzType, m)) {
	    String fqn = clzType.getClassName ();
	    String name = m.getId ();
	    ArgumentList al = m.getArgumentList ();
	    try {
		List<MethodInformation> mis = findMatchingMethod (fqn, name, al);
		if (mis == null) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
								 "No matching method found for: %s(%s)",
								 name, argList (al)));
		} else if (mis.size () > 1) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
								 "Multiple matching methods found for: %s(%s)",
								 name, argList (al)));
		} else {
		    m.setMethodInformation (mis.get (0));
		}
	    } catch (IOException e) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     m.getParsePosition (),
							     "Failed to read class: " + e));
	    }
	}
	return false;
    }

    private final String argList (ArgumentList al) {
	if (al == null)
	    return "";
	return al.get ().stream ().map (tn -> tn.getExpressionType ().toString ()).collect (Collectors.joining (", "));
    }

    private ExpressionType getOnType (TreeNode on) {
	if (on != null) {
	    return on.getExpressionType ();
	} else {
	    String fqn = cip.getFullName (containingClasses.getLast ());
	    return new ExpressionType (fqn);
	}
    }

    private boolean hasMethods (ExpressionType clzType, TreeNode m) {
	if (clzType.isPrimitiveType ()) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 m.getParsePosition (),
							 "Primitives have no methods"));
	    return false;
	} else if (clzType == ExpressionType.VOID) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 m.getParsePosition (),
							 "Void has no methods"));
	    return false;
	} else if (clzType == ExpressionType.NULL) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 m.getParsePosition (),
							 "null has no methods"));
	    return false;
	}
	return true;
    }

    private List<MethodInformation> findMatchingConstructor (String fqn, ArgumentList al)
	throws IOException {
	return findMatching (fqn, al, new ConstructorMethodFinder ());
    }

    private List<MethodInformation> findMatchingMethod (String fqn, String name, ArgumentList al) throws IOException {
	return findMatching (fqn, al, new MethodMethodFinder (name));
    }

    private List<MethodInformation> findMatching (String fqn, ArgumentList al, MethodFinder mf) throws IOException {
	List<MethodInformation> ret = null;

	Deque<String> typesToCheck = new ArrayDeque<> ();
	typesToCheck.addLast (fqn);
	Set<String> visitedTypes = new HashSet<> ();
	Set<String> matchedDesc = Collections.emptySet ();
	while (!typesToCheck.isEmpty ()) {
	    String clz = typesToCheck.removeFirst ();
	    if (visitedTypes.contains (clz))
		continue;
	    visitedTypes.add (clz);
	    List<MethodInformation> ls = mf.getAlternatives (clz);
	    if (ls != null) {
		for (MethodInformation mi : ls) {
		    if (!matchedDesc.contains (mi.getDesc ()) && match (al, mi)) {
			if (ret == null) {
			    ret = new ArrayList<> ();
			    matchedDesc = new HashSet<> ();
			}
			ret.add (mi);
			matchedDesc.add (mi.getDesc ());
		    }
		}
	    }
	    // not found in current class, try super class
	    Optional<List<String>> o = cip.getSuperTypes (clz);
	    if (o.isPresent () && mf.mayUseSuperclassMethods ())
		typesToCheck.addAll (o.get ());
	}
	return ret;
    }

    private boolean match (ArgumentList al, MethodInformation mi) {
	if (mi.isVarArgs ()) {
	    if (match (al, mi, mi.getNumberOfArguments () - 1))
		return matchVarArg (al, mi);
	} else {
	    int numArgs = mi.getNumberOfArguments ();
	    int argListSize = al == null ? 0 : al.size ();
	    return numArgs == argListSize && match (al, mi, numArgs);
	}
	return false;
    }

    private boolean match (ArgumentList al, MethodInformation mi, int numArguments) {
	if (al == null)
	    return numArguments == 0;
	Type[] arguments = mi.getArguments ();
	if (al.size () < arguments.length)
	    return false;
	for (int i = 0; i < numArguments; i++) {
	    TreeNode tn = al.get ().get (i);
	    ExpressionType et = tn.getExpressionType ();
	    if (!et.match (arguments[i++]))
		return false;
	}
	return true;
    }

    private boolean matchVarArg (ArgumentList al, MethodInformation mi) {
	if (al == null) // no need to check that we have matched previous parts
	    return true;
	Type[] arguments = mi.getArguments ();
	int varArgStart = arguments.length - 1;
	Type t = mi.getArguments ()[varArgStart].getElementType ();
	for (int i = varArgStart; i < al.size (); i++) {
	    TreeNode tn = al.get ().get (i);
	    ExpressionType et = tn.getExpressionType ();
	    if (!et.match (t))
		return false;
	}
	return true;
    }

    private abstract class MethodFinder {
	protected final String methodName;

	public MethodFinder (String methodName) {
	    this.methodName = methodName;
	}

	public abstract List<MethodInformation> getAlternatives (String clz);

	public abstract boolean mayUseSuperclassMethods ();
    }

    private class MethodMethodFinder extends MethodFinder {

	public MethodMethodFinder (String methodName) {
	    super (methodName);
	}

	public List<MethodInformation> getAlternatives (String clz) {
	    Map<String, List<MethodInformation>> methods = cip.getMethods (clz);
	    if (methods != null)
		return methods.get (methodName);
	    return null;
	}

	public boolean mayUseSuperclassMethods () {
	    return true;
	}
    }

    private static final List<MethodInformation> DEFAULT_CONSTRUCTOR_ONLY =
	Collections.singletonList (new MethodInformation (null, Opcodes.ACC_PUBLIC, "<init>", "()V", null, null));

    private class ConstructorMethodFinder extends MethodFinder {
	public ConstructorMethodFinder () {
	    super ("<init>");
	}

	public List<MethodInformation> getAlternatives (String clz) {
	    Map<String, List<MethodInformation>> methods = cip.getMethods (clz);
	    List<MethodInformation> ls = null;
	    if (methods != null)
		ls = methods.get (methodName);
	    if (ls == null)
		return DEFAULT_CONSTRUCTOR_ONLY;
	    return ls;
	}

	public boolean mayUseSuperclassMethods () {
	    return false;
	}
    }

    @Override public boolean visit (FieldAccess f) {
	ExpressionType retType = cip.getFieldType (f.getFrom ().getExpressionType ().getClassName (),
						   f.getFieldId ());
	if (retType == null) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 f.getParsePosition (),
							 "Cannot find symbol: %s: %s",
							 f.getFieldId (), f));
	} else {
	    f.setReturnType (retType);
	}
	return true;
    }
}
