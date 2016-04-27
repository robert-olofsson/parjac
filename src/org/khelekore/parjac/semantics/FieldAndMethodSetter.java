package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.ArgumentList;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassInstanceCreationExpression;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExplicitConstructorInvocation;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.FieldAccess;
import org.khelekore.parjac.tree.MethodInformationHolder;
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

    @Override public boolean visit (ExplicitConstructorInvocation eci) {
	String fqn = cip.getFullName (containingClasses.getLast ());
	try {
	    Optional<List<String>> supers = cip.getSuperTypes (fqn, false);
	    if (supers.isPresent ())
		fqn = supers.get ().get (0);
	    else
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     eci.getParsePosition (),
							     "No superclass for: %s", fqn));
	} catch (IOException e) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 eci.getParsePosition (),
							 "Failed to read class: " + e));
	}
	ArgumentList al = eci.getArguments ().getArgumentList ();
	findConstructor (fqn, al, eci);
	return false;
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
	    findConstructor (fqn, al, c);
	}
	return true;
    }

    private void findConstructor (String fqn, ArgumentList al, MethodInformationHolder c) {
	try {
	    FindResult fr = findMatchingConstructor (fqn, al);
	    if (fr == null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), c.getParsePosition (),
							     "No matching constructor found for %s(%s)",
							     fqn, argList (al)));
	    } else if (fr.hasMoreThanOneMatch ()) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), c.getParsePosition (),
							     "Multiple matching constructor found for %s(%s)",
							     fqn, argList (al)));
	    } else {
		c.setMethodInformation (fr.get ());
	    }
	} catch (IOException e) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							 c.getParsePosition (),
							 "Failed to read class: " + e));
	}
    }

    @Override public boolean visit (MethodInvocation m) {
	ExpressionType clzType = getOnType (m.getOn ());
	if (hasMethods (clzType, m)) {
	    String fqn = clzType.getClassName ();
	    boolean isArray = clzType.isArray ();
	    String name = m.getId ();
	    ArgumentList al = m.getArgumentList ();
	    try {
		FindResult fr = findMatchingMethod (fqn, isArray, name, al);
		if (fr == null) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
								 "No matching method found for: %s(%s)",
								 name, argList (al)));
		} else if (fr.hasMoreThanOneMatch ()) {
		    String options = fr.getAlternatives ().stream ()
			.map (mi -> mi.getClassname () + "::" + mi.getName () + "(" + args (mi.getArguments ()) + ")")
			.collect (Collectors.joining ("\n\t", "\n\t", "\n"));
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), m.getParsePosition (),
								 "Multiple matching methods found for: %s(%s)%s",
								 name, argList (al), options));
		} else {
		    m.setMethodInformation (fr.get ());
		}
	    } catch (IOException e) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (),
							     m.getParsePosition (),
							     "Failed to read class: " + e));
	    }
	}
	return false;
    }

    private String args (Type[] types) {
	return Arrays.stream (types).map (t -> t.toString ()).collect (Collectors.joining (", "));
    }

    private String argList (ArgumentList al) {
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

    private FindResult findMatchingConstructor (String fqn, ArgumentList al)
	throws IOException {
	return findMatching (fqn, false, al, new ConstructorMethodFinder ());
    }

    private FindResult findMatchingMethod (String fqn, boolean isArray,
					   String name, ArgumentList al) throws IOException {
	return findMatching (fqn, isArray, al, new MethodMethodFinder (name));
    }

    private FindResult findMatching (String fqn, boolean isArray,
				     ArgumentList al, MethodFinder mf) throws IOException {
	FindResult fr = null;
	Deque<String> typesToCheck = new ArrayDeque<> ();
	typesToCheck.addLast (fqn);
	Set<String> visitedTypes = new HashSet<> ();
	Set<List<Type>> matchedDesc = Collections.emptySet ();
	while (!typesToCheck.isEmpty ()) {
	    String clz = typesToCheck.removeFirst ();
	    if (visitedTypes.contains (clz))
		continue;
	    visitedTypes.add (clz);
	    List<MethodInformation> ls = mf.getAlternatives (clz, isArray);
	    if (ls != null) {
		for (MethodInformation mi : ls) {
		    if (FlagsHelper.isSynthetic (mi.getAccess ()))
			continue;
		    List<Type> argumentList = Arrays.asList (mi.getArguments ());
		    if (!matchedDesc.contains (argumentList)) {
			MatchType t = match (al, mi);
			if (t.isValid ()) {
			    if (fr == null) {
				fr = new FindResult ();
				matchedDesc = new HashSet<> ();
			    }
			    fr.add (t, mi);
			    matchedDesc.add (argumentList);
			}
		    }
		}
	    }
	    // not found in current class, try super class
	    Optional<List<String>> o = cip.getSuperTypes (clz, isArray);
	    if (o.isPresent () && mf.mayUseSuperclassMethods ())
		typesToCheck.addAll (o.get ());
	    isArray = false;
	}
	return fr;
    }

    private class FindResult {
	private List<MethodInformation> matched;
	private List<MethodInformation> autoboxMatched;
	private List<MethodInformation> varargMatched;

	public void add (MatchType t, MethodInformation mi) {
	    if (t == MatchType.MATCH) {
		if (matched == null)
		    matched = new ArrayList<> ();
		add (mi, matched);
	    } else if (t == MatchType.AUTOBOX_MATCH) {
		if (autoboxMatched == null)
		    autoboxMatched = new ArrayList<> ();
		add (mi, autoboxMatched);
	    } else if (t == MatchType.VARARG_MATCH) {
		if (varargMatched == null)
		    varargMatched = new ArrayList<> ();
		add (mi, varargMatched);
	    }
	}

	private void add (MethodInformation mi, List<MethodInformation> mis) {
	    for (int i = mis.size () - 1; i >= 0; i--) {
		if (isSpecialization (mi, mis.get (i)))
		    mis.remove (i);
		else if (isSpecialization (mis.get (i), mi))
		    return;
	    }
	    mis.add (mi);
	}

	private boolean isSpecialization (MethodInformation mi, MethodInformation o) {
	    Type[] ts1 = mi.getArguments ();
	    Type[] ts2 = o.getArguments ();
	    try {
		for (int i = 0; i < ts1.length; i++) {
		    ExpressionType t1 = ExpressionType.get (ts1[i]);
		    ExpressionType t2 = ExpressionType.get (ts2[i]);
		    boolean specialization = false;
		    if (t1.isPrimitiveType ())
			specialization = ExpressionType.mayBeAutoCasted (t1, t2);
		    else
			specialization = cip.isSubType (t1, t2);
		    if (!specialization)
			return false;
		}
	    } catch (IOException e) {
		diagnostics.report (new NoSourceDiagnostics ("Failed to load classes for %s and %s: %s", mi, o, e));
	    }
	    return true;
	}

	public boolean hasMoreThanOneMatch () {
	    if (matched != null)
		return matched.size () > 1;
	    if (autoboxMatched != null)
		return autoboxMatched.size () > 1;
	    if (varargMatched != null)
		return varargMatched.size () > 1;
	    throw new IllegalStateException ("No matched results");
	}

	public MethodInformation get () {
	    if (matched != null)
		return matched.get (0);
	    if (autoboxMatched != null)
		return autoboxMatched.get (0);
	    if (varargMatched != null)
		return varargMatched.get (0);
	    throw new IllegalStateException ("No matched results");
	}

	public List<MethodInformation> getAlternatives () {
	    if (matched != null)
		return matched;
	    if (autoboxMatched != null)
		return autoboxMatched;
	    if (varargMatched != null)
		return varargMatched;
	    throw new IllegalStateException ("No matched results");
	}
    }

    private enum MatchType {
	MATCH (true), AUTOBOX_MATCH (true), VARARG_MATCH (true), NO_MATCH (false);

	private final boolean valid;

	private MatchType (boolean valid) {
	    this.valid = valid;
	}

	public boolean isValid () {
	    return valid;
	}
    }

    private MatchType match (ArgumentList al, MethodInformation mi) {
	if (mi.isVarArgs ()) {
	    if (match (al, mi, mi.getNumberOfArguments () - 1).isValid ())
		return matchVarArg (al, mi);
	} else {
	    int numArgs = mi.getNumberOfArguments ();
	    int argListSize = al == null ? 0 : al.size ();
	    if (numArgs == argListSize)
		return match (al, mi, numArgs);
	}
	return MatchType.NO_MATCH;
    }

    private MatchType match (ArgumentList al, MethodInformation mi, int numArguments) {
	if (al == null)
	    return numArguments == 0 ? MatchType.MATCH : MatchType.NO_MATCH;
	Type[] arguments = mi.getArguments ();
	if (al.size () < arguments.length)
	    return MatchType.NO_MATCH;
	for (int i = 0; i < numArguments; i++) {
	    TreeNode tn = al.get ().get (i);
	    ExpressionType et = tn.getExpressionType ();
	    if (!et.match (arguments[i++]))
		return MatchType.NO_MATCH;
	}
	return MatchType.MATCH;
    }

    private MatchType matchVarArg (ArgumentList al, MethodInformation mi) {
	if (al == null) // no need to check that we have matched previous parts
	    return MatchType.VARARG_MATCH;
	Type[] arguments = mi.getArguments ();
	int varArgStart = arguments.length - 1;
	Type t = mi.getArguments ()[varArgStart].getElementType ();
	for (int i = varArgStart; i < al.size (); i++) {
	    TreeNode tn = al.get ().get (i);
	    ExpressionType et = tn.getExpressionType ();
	    if (!et.match (t))
		return MatchType.NO_MATCH;
	}
	return MatchType.VARARG_MATCH;
    }

    private abstract class MethodFinder {
	protected final String methodName;

	public MethodFinder (String methodName) {
	    this.methodName = methodName;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + methodName + "}";
	}

	public abstract List<MethodInformation> getAlternatives (String clz, boolean isArray);

	public abstract boolean mayUseSuperclassMethods ();
    }

    private class MethodMethodFinder extends MethodFinder {

	public MethodMethodFinder (String methodName) {
	    super (methodName);
	}

	public List<MethodInformation> getAlternatives (String clz, boolean isArray) {
	    if (isArray) {
		if (methodName.equals ("clone")) {
		    MethodInformation mi = new MethodInformation (clz, Opcodes.ACC_PUBLIC, "clone",
								  "()Ljava/lang/Object;", null, null);
		    return Collections.singletonList (mi);
		}
		return null;
	    }
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

	public List<MethodInformation> getAlternatives (String clz, boolean isArray) {
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
