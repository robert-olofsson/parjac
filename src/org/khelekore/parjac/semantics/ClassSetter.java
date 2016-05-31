package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.*;

public class ClassSetter {
    private final ClassInformationProvider cip;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final DottedName packageName;
    private final ImportHandler ih = new ImportHandler ();
    private Deque<BodyPart> containingTypes = new ArrayDeque<> ();
    private final Map<TreeNode, TypeHolder> types = new HashMap<> ();
    private TypeHolder currentTypes = null;

    private final static String[] EMTPY = new String[0];

    /** High level description:
     *  For each class:
     *      Setup a Scope
     *      Find fields, set type on them, add to scope of class
     *      Find methods, set type on arguments and add to scope of method
     *      Set type of expression
     */
    public static void fillInClasses (ClassInformationProvider cip,
				      List<SyntaxTree> trees,
				      CompilerDiagnosticCollector diagnostics) {
	List<ClassSetter> classSetters =
	    trees.stream ().map (t -> new ClassSetter (cip, t, diagnostics)).collect (Collectors.toList ());
	if (diagnostics.hasError ())
	    return;

	classSetters.parallelStream ().forEach (cs -> cs.registerSuperTypes ());

	// Fill in correct classes
	// Depending on order we may not have correct parents on first try.
	// We collect the trees that fails and tries again
	Queue<ClassSetter> rest = new ConcurrentLinkedQueue<> ();
	classSetters.parallelStream ().forEach (cs -> cs.addScopes (rest, 0));
	// TODO: we probably need to loop until rest is empty
	if (!rest.isEmpty ()) {
	    Queue<ClassSetter> rest2 = new ConcurrentLinkedQueue<> ();
	    rest.parallelStream ().forEach (cs -> cs.addScopes (rest2, 1));
	}
	if (diagnostics.hasError ())
	    return;
	classSetters.parallelStream ().forEach (cs -> cs.checkUnusedImport ());
    }

    public ClassSetter (ClassInformationProvider cip, SyntaxTree tree,
			CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.tree = tree;
	this.diagnostics = diagnostics;

	CompilationUnit cu = tree.getCompilationUnit ();
	packageName = cu.getPackage ();
	ih.addDefaultPackages ();
	cu.getImports ().forEach (i -> i.visit (ih));
    }

    public void registerSuperTypes () {
	SuperTypeRegistrator str = new SuperTypeRegistrator ();
	tree.getCompilationUnit ().visit (str);
    }

    private class SuperTypeRegistrator implements TreeVisitor, ErrorHandler {
	@Override public boolean visit (NormalClassDeclaration c) {
	    pushType (c, c.getBody (), c.getTypeParameters ());
	    ClassType superclass = c.getSuperClass ();
	    if (superclass != null)
		setType (superclass, this);
	    visitSuperInterfaces (c.getSuperInterfaces (), this);
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    pushType (e, e.getBody (), null);
	    visitSuperInterfaces (e.getSuperInterfaces (), this);
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    pushType (i, i.getBody (), i.getTypeParameters ());
	    ExtendsInterfaces ei = i.getExtendsInterfaces ();
	    if (ei != null)
		visitSuperInterfaces (ei.get (), this);
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    pushType (a, a.getBody (), null);
	    return true;
	}

	private void pushType (TreeNode type, TreeNode body, TypeParameters tps) {
	    String fqn = cip.getFullName (type);
	    containingTypes.push (new BodyPart (fqn, body));
	    registerTypeParameters (type, tps, this);
	}

	@Override public void endType () {
	    containingTypes.pop ();
	    currentTypes = currentTypes.parent;
	}

	@Override public boolean mayReport () {
	    return true;
	}

	@Override public void setIncomplete () {
	    // ignore
	}
    }

    public void addScopes (Queue<ClassSetter> rest, int level) {
	ScopeSetter ss = new ScopeSetter (level);
	tree.getCompilationUnit ().visit (ss);
	if (!ss.completed ())
	    rest.add (this);
    }

    public void checkUnusedImport () {
	List<ImportDeclaration> imports = tree.getCompilationUnit ().getImports ();
	imports.stream ().filter (i -> !i.hasBeenUsed ()).forEach (i -> checkUnusedImport (i));
    }

    private void checkUnusedImport (ImportDeclaration i) {
	if (i instanceof SingleStaticImportDeclaration) {
	    SingleStaticImportDeclaration si = (SingleStaticImportDeclaration)i;
	    String full = si.getFullName ();
	    String fqn = si.getName ().getDotName ();
	    String field = si.getInnerId ();
	    if (!hasVisibleType (full) && cip.getFieldInformation (fqn, field) == null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getParsePosition (),
							     "Type %s has no symbol: %s", fqn, field));
		return;
	    }
	}
	diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (),
						       i.getParsePosition (),
						       "Unused import"));
    }

    private class ScopeSetter implements TreeVisitor, ErrorHandler {
	private final int level;
	private Scope currentScope = null;
	private boolean completed = true;

	public ScopeSetter (int level) {
	    this.level = level;
	}

	@Override public boolean mayReport () {
	    return level > 0;
	}

	@Override public void setIncomplete () {
	    completed = false;
	}

	public boolean completed () {
	    return completed;
	}

	@Override public boolean visit (NormalClassDeclaration c) {
	    String fqn = cip.getFullName (c);
	    pushType (c, fqn, c.getBody ());
	    addScopeAndFields (fqn, c, c.getBody ());
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    String fqn = cip.getFullName (e);
	    pushType (e, fqn, e.getBody ());
	    addScopeAndFields (fqn, e, e.getBody ());
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    String fqn = cip.getFullName (i);
	    pushType (i, fqn, i.getBody ());
	    addScopeAndFields (fqn, i, i.getBody ());
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    String fqn = cip.getFullName (a);
	    pushType (a, fqn, a.getBody ());
	    addScopeAndFields (fqn, a, a.getBody ());
	    return true;
	}

	private void pushType (TreeNode type, String fqn, TreeNode body) {
	    containingTypes.push (new BodyPart (fqn, body));
	    currentTypes = types.get (type);
	}

	@Override public void endType () {
	    if (completed)
		registerMethods (containingTypes.pop ());
	    currentScope = currentScope.endScope ();
	    currentTypes = currentTypes.parent;
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    addScope (b, Scope.Type.CLASS);
	    if (ct.getFullName () != null) {
		containingTypes.push (new BodyPart (ct.getFullName (), null));
		String fqn = cip.getFullName (b);
		containingTypes.push (new BodyPart (fqn, b));
		return true;
	    }
	    // No need to continue here
	    // TODO: do we need to produce errors or warnings?
	    return false;
	}

	@Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	    if (ct.getFullName () != null) {
		registerMethods (containingTypes.pop ());
		containingTypes.pop ();
	    }
	    currentScope = currentScope.endScope ();
	}

	// TODO: handle InstanceInitializer and StaticInitializer, they need scope

	@Override public boolean visit (ConstructorDeclaration c) {
	    registerTypeParameters (c, c.getTypeParameters (), this);
	    setTypes (c.getParameters (), this);
	    setThrowsTypes (c.getThrows ());
	    addScopeAndParameters (c, c.getParameters ());
	    // TODO: store method information
	    return true;
	}

	@Override public void endConstructor (ConstructorDeclaration c) {
	    currentScope = currentScope.endScope ();
	    currentTypes = currentTypes.parent;
	}

	@Override public boolean visit (FieldDeclaration f) {
	    setType (f.getType (), ScopeSetter.this);
	    return true;
	}

	@Override public boolean visit (EnumConstant c) {
	    c.setType (containingTypes.peek ().fqn);
	    return true;
	}

	@Override public boolean visit (MethodDeclaration m) {
	    registerTypeParameters (m, m.getTypeParameters (), this);
	    Result r = m.getResult ();
	    if (r instanceof Result.TypeResult)
		setType (r.getReturnType (), this);
	    setTypes (m.getParameters (), this);
	    setThrowsTypes (m.getThrows ());
	    addScopeAndParameters (m, m.getParameters ());
	    return true;
	}

	private void setThrowsTypes (Throws t) {
	    if (t != null)
		for (ClassType ct : t.getTypes ())
		    setType (ct, this);
	}

	@Override public void endMethod (MethodDeclaration m) {
	    currentScope = currentScope.endScope ();
	    currentTypes = currentTypes.parent;
	}

	@Override public boolean visit (Block b) {
	    addScope (b, Scope.Type.LOCAL);
	    return true;
	}

	@Override public void endBlock () {
	    currentScope = currentScope.endScope ();
	}

	@Override public boolean visit (ClassInstanceCreationExpression c) {
	    TreeNode from = c.getFrom ();
	    if (from == null) {
		setType (c.getId (), this);
	    } else {
		from.visit (this);
		replaceAndSetType (from);
		if (from.getExpressionType () != null) {
		    Deque<BodyPart> save = containingTypes;
		    containingTypes = new ArrayDeque<> ();
		    ExpressionType type = from.getExpressionType ();
		    if (type != null)
			containingTypes.push (new BodyPart (type.getClassName (), null));
		    setType (c.getId (), this);
		    containingTypes = save;
		} else {
		    setIncomplete ();
		    if (mayReport ())
			diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), c.getParsePosition (),
								     "Unknown expression type for: %s", from));
		}
	    }
	    return true;
	}

	@Override public boolean visit (MethodInvocation m) {
	    TreeNode on = m.getOn ();
	    if (on != null)
		replaceAndSetType (on);
	    ArgumentList al = m.getArgumentList ();
	    if (al != null) {
		List<TreeNode> ls = al.get ();
		for (int i = 0, s = ls.size (); i < s; i++) {
		    replaceAndSetType (ls.get (i));
		}
	    }
	    return true;
	}

	@Override public boolean visit (ArrayCreationExpression ace) {
	    setType (ace.getType (), this);
	    return true;
	}

	@Override public boolean visit (LambdaExpression l) {
	    TreeNode params = l.getParameters ();
	    if (params != null) {
		addScope (l, Scope.Type.LOCAL);
		if (params instanceof Identifier) {
		    currentScope.tryToAdd (cip, new LambdaParameter ((Identifier)params), tree, diagnostics);
		} else if (params instanceof InferredFormalParameterList) {
		    InferredFormalParameterList ls = (InferredFormalParameterList)params;
		    for (Identifier i : ls.getIdentifiers ()) {
			currentScope.tryToAdd (cip, new LambdaParameter (i), tree, diagnostics);
		    }
		} else if (params instanceof FormalParameterList) {
		    addParameterList ((FormalParameterList)params);
		}
	    }
	    return true;
	}

	@Override public void endLambda (LambdaExpression l) {
	    TreeNode params = l.getParameters ();
	    if (params != null)
		currentScope = currentScope.endScope ();
	}

	@Override public boolean visit (LocalVariableDeclaration l) {
	    setType (l.getType (), this);
	    for (VariableDeclarator v : l.getVariables ().get ()) {
		TreeNode tn = v.getInitializer ();
		if (tn != null)
		    tn.visit (this);
		currentScope.tryToAdd (cip, l, v, tree, diagnostics);
	    }
	    return false;
	}

	@Override public boolean visit (CastExpression c) {
	    setType (c.getType (), this);
	    return true;
	}

	@Override public void visit (Identifier i) {
	    String id = i.get ();
	    Scope.FindResult fr = currentScope.find (cip, id, currentScope.isStatic (), true, diagnostics);
	    if (fr == null) {
		StaticFieldAccess sfa = findStaticImportedField (id);
		if (sfa != null) {
		    ClassType from = getClassType (sfa.fqn, i.getParsePosition ());
		    FieldAccess fa = new FieldAccess (from, id, i.getParsePosition ());
		    fa.setReturnType (sfa.field.getExpressionType ());
		    i.setActual (fa);
		} else {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getParsePosition (),
								 "Cannot find symbol: %s", i.get ()));
		}
	    }
	}

	private StaticFieldAccess findStaticImportedField (String name) {
	    ImportHandler.StaticImportType sit = ih.ssid.get (name);
	    String fqn = null;
	    if (sit != null) {
		fqn = sit.containingClass;
		FieldInformation<?> fi = cip.getFieldInformation (fqn, name);
		if (fi != null) {
		    sit.ssid.markUsed ();
		    return new StaticFieldAccess (fqn, fi);
		}
	    }
	    for (StaticImportOnDemandDeclaration siod : ih.siod) {
		fqn = siod.getName ().getDotName ();
		FieldInformation<?> fi = cip.getFieldInformation (fqn, name);
		if (fi != null) {
		    siod.markUsed ();
		    return new StaticFieldAccess (fqn, fi);
		}
	    }
	    return null;
	}

	@Override public void visit (DottedName d) {
	    d.setChainedFieldAccess (replaceWithChainedFieldAccess (d, currentScope, this));
	}

	private TreeNode replaceWithChainedFieldAccess (DottedName dn, Scope scope, ErrorHandler eh) {
	    TreeNode current = null;
	    final ParsePosition pos = dn.getParsePosition ();
	    StringBuilder sb = new StringBuilder ();
	    ClassType ct = null;
	    List<String> parts = dn.getParts ();
	    for (int i = 0, s = parts.size (); i < s; i++) {
		String p = parts.get (i);
		if (i == 0) {
		    Scope.FindResult fr = scope.find (cip, p, scope.isStatic (), true, diagnostics);
		    if (fr != null) {
			current = getFieldOrLocalAccess (p, pos, fr);
			continue;
		    }
		} else {
		    sb.append (".");
		}
		sb.append (p);
		ResolvedClass outerClass = ct == null ? resolve (sb.toString (), pos, eh) : null;
		if (outerClass != null) {
		    DottedName pdn = new DottedName (parts.subList (0, i + 1), pos);
		    ct = pdn.toClassType ();
		    ct.setFullName (outerClass.type);
		    ct.setTypeParameter (outerClass.tp);
		    current = ct;
		    continue;
		}
		if (current != null) {
		    current = new FieldAccess (current, p, pos);
		} else {
		    current = new Identifier (p, pos);
		}
	    }
	    return current;
	}

	private TreeNode getFieldOrLocalAccess (String p, ParsePosition pos, Scope.FindResult fr) {
	    FieldInformation<?> fi = fr.fi;
	    if (!isField (fi))
		return new Identifier (p, pos, fi.getExpressionType ());
	    FieldDeclaration fd = (FieldDeclaration)fi.getVariableDeclaration ();
	    if (FlagsHelper.isStatic (fd.getFlags ())) {
		FieldAccess fa = new FieldAccess (getStatic (fi.getOwner (), pos), p, pos);
		fa.setReturnType (fi.getExpressionType ());
		return fa;
	    }

	    if (fr.containingClass == cip.getType (containingTypes.peek ().fqn)) {
		TreeNode from = fi.getOwner () == fr.containingClass ? getThis (pos) : getSuperThis (pos, cip.getFullName (fi.getOwner ()));
		FieldAccess fa = new FieldAccess (from, p, pos);
		fa.setReturnType (fi.getExpressionType ());
		return fa;
	    }
	    FieldAccess fa = new FieldAccess (getDottedThis (fi.getOwner (), pos), p, pos);
	    fa.setReturnType (fi.getExpressionType ());
	    return fa;
	}

	private boolean isField (FieldInformation<?> fi) {
	    return fi.getVariableDeclaration () instanceof FieldDeclaration;
	}

	private TreeNode getThis (ParsePosition pos) {
	    PrimaryNoNewArray.ThisPrimary t = new PrimaryNoNewArray.ThisPrimary (pos);
	    t.setExpressionType (ExpressionType.getObjectType (containingTypes.peek ().fqn));
	    return t;
	}

	private TreeNode getSuperThis (ParsePosition pos, String fqn) {
	    return new PrimaryNoNewArray.SuperThisPrimary (pos, fqn);
	}

	private TreeNode getStatic (TreeNode owner, ParsePosition pos) {
	    String fqn = cip.getFullName (owner);
	    return getClassType (fqn, pos);
	}

	private TreeNode getDottedThis (TreeNode owner, ParsePosition pos) {
	    String fqn = cip.getFullName (owner);
	    ClassType ct = getClassType (fqn, pos);
	    return new PrimaryNoNewArray.DottedThis (ct, pos);
	}

	@Override public boolean visit (BasicForStatement f) {
	    addScope (f, Scope.Type.LOCAL);
	    return true;
	}

	@Override public boolean visit (EnhancedForStatement f) {
	    addScope (f, Scope.Type.LOCAL);
	    return true;
	}

	@Override public void endFor () {
	    currentScope = currentScope.endScope ();
	}

	@Override public boolean visit (SwitchBlock s) {
	    addScope (s, Scope.Type.LOCAL);
	    return true;
	}

	@Override public  void endSwitchBlock () {
	    currentScope = currentScope.endScope ();
	}

	@Override public boolean visit (CatchClause c) {
	    addScope (c, Scope.Type.LOCAL);
	    CatchFormalParameter cfp = c.getFormalParameter ();
	    currentScope.tryToAdd (cip, cfp, tree, diagnostics);
	    return true;
	}

	@Override public void endCatchClause (CatchClause c) {
	    currentScope = currentScope.endScope ();
	}

	@Override public boolean visit (CatchType c) {
	    c.getTypes ().forEach (ct -> setType (ct, this));

	    List<ClassType> cts = c.getTypes ();
	    ExpressionType et = cts.get (0).getExpressionType ();
	    for (int i = 1, s = cts.size (); i < s; i++) {
		// TODO: find common super class
		et = ExpressionType.getObjectType ("java.lang.Exception");
	    }
	    c.setExpressionType (et);

	    return true;
	}

	private void replaceAndSetType (TreeNode tn) {
	    if (tn instanceof Identifier && tn.getExpressionType () == null) {
		Identifier i = (Identifier)tn;
		Scope.FindResult fr = currentScope.find (cip, i.get (), currentScope.isStatic (), true, diagnostics);
		if (fr != null) {
		    i.setActual (new Identifier (i.get (), i.getParsePosition (), fr.fi.getExpressionType ()));
		}
	    }
	}

	private void addScopeAndFields (String fqn, FlaggedType ft, TreeNode part) {
	    addScope (ft, Scope.Type.CLASS, false);
	}

	private void addScopeAndParameters (FlaggedType ft, FormalParameterList fpl) {
	    addScope (ft, Scope.Type.LOCAL, true);
	    addParameterList (fpl);
	}

	private void addParameterList (FormalParameterList fpl) {
	    if (fpl != null) {
		NormalFormalParameterList pl = fpl.getParameters ();
		if (pl != null) {
		    List<FormalParameter> ls = pl.getFormalParameters ();
		    if (ls != null)
			ls.forEach (fp -> currentScope.tryToAdd (cip, fp, tree, diagnostics));
		    LastFormalParameter lfp = pl.getLastFormalParameter ();
		    if (lfp != null) {
			currentScope.tryToAdd (cip, lfp, tree, diagnostics);
		    }
		}
	    }
	}

	private void addScope (FlaggedType ft, Scope.Type type, boolean ignoreFieldShadowing) {
	    currentScope = new Scope (ft, currentScope, type, FlagsHelper.isStatic (ft.getFlags ()), ignoreFieldShadowing);
	}

	private void addScope (TreeNode tn, Scope.Type type) {
	    boolean isStatic = currentScope != null && currentScope.isStatic ();
	    currentScope = new Scope (tn, currentScope, type, isStatic, false);
	}

	private void registerMethods (BodyPart bp) {
	    MethodRegistrator mr = new MethodRegistrator (bp.fqn);
	    bp.body.visit (mr);
	    cip.registerMethods (bp.fqn, mr.methods);
	}

	private class MethodRegistrator extends SiblingVisitor {
	    private final String fqn;
	    private final Map<String, List<MethodInformation>> methods = new HashMap<> ();

	    public MethodRegistrator (String fqn) {
		this.fqn = fqn;
	    }

	    @Override public boolean visit (ConstructorDeclaration c) {
		addMethod (new MethodInformation (fqn,
						  c.getFlags (),
						  "<init>",
						  c.getDescription (),
						  null,   // TODO: fill in
						  EMTPY)); // TODO: fill in
		return false;
	    }

	    @Override public boolean visit (MethodDeclaration m) {
		addMethod (new MethodInformation (fqn,
						  m.getFlags (),
						  m.getMethodName (),
						  m.getDescription (),
						  null,   // TODO: fill in
						  EMTPY)); // TODO: fill in
		return false;
	    }

	    private void addMethod (MethodInformation mi) {
		String name = mi.getName ();
		List<MethodInformation> ls = methods.get (name);
		if (ls == null) {
		    ls = new ArrayList<> ();
		    methods.put (name, ls);
		}
		ls.add (mi);
	    }
	}

	@Override public void visit (PrimaryNoNewArray.ThisPrimary t) {
	    t.setExpressionType (ExpressionType.getObjectType (containingTypes.peek ().fqn));
	}
    }

    private static class BodyPart {
	private final String fqn;
	private final TreeNode body;

	public BodyPart (String fqn, TreeNode body) {
	    this.fqn = fqn;
	    this.body = body;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + fqn + ", " + body + "}";
	}
    }

    private void visitSuperInterfaces (InterfaceTypeList superInterfaces, ErrorHandler eh) {
	if (superInterfaces != null)
	    setTypes (superInterfaces, eh);
    }

    private void registerTypeParameters (TreeNode tn, TypeParameters tps, ErrorHandler eh) {
	Map<String, TypeParameter> tt;
	if (tps != null) {
	    tt = new HashMap<> ();
	    for (TypeParameter tp : tps.get ()) {
		cip.registerTypeParameter (containingTypes.peek ().fqn, tp);
		tt.put (tp.getId (), tp);
		checkClasses (tp, eh);
	    }
	} else {
	    tt = Collections.emptyMap ();
	}
	currentTypes = new TypeHolder (currentTypes, tt);
	types.put (tn, currentTypes);
    }

    private void checkClasses (TypeParameter tp, ErrorHandler eh) {
	TypeBound b = tp.getTypeBound ();
	if (b != null) {
	    setType (b.getType (), eh);
	    List<AdditionalBound> ls = b.getAdditionalBounds ();
	    if (ls != null) {
		ls.forEach (ab -> setType (ab.getType (), eh));
	    }
	}
    }

    private void setTypes (InterfaceTypeList ls, ErrorHandler eh) {
	if (ls == null)
	    return;
	for (ClassType ct : ls.get ())
	    setType (ct, eh);
    }

    private void setTypes (FormalParameterList ls, ErrorHandler eh) {
	if (ls == null)
	    return;

	NormalFormalParameterList fps = ls.getParameters ();
	if (fps != null) {
	    List<FormalParameter> args = fps.getFormalParameters ();
	    if (args != null) {
		for (FormalParameter fp : args)
		    setType (fp.getType (), eh);
	    }
	    LastFormalParameter lfp = fps.getLastFormalParameter ();
	    if (lfp != null) {
		setType (lfp.getType (), eh);
	    }
	}
    }

    private void setType (TreeNode type, ErrorHandler eh) {
	if (type instanceof PrimitiveTokenType) {
	    return;
	}
	if (type instanceof PrimitiveType) {
	    return;
	}
	if (type instanceof ClassType) {
	    ClassType ct = (ClassType)type;
	    if (ct.getFullName () != null) // already set?
		return;
	    // "List" and "java.util.List" are easy to resolve
	    String id1 = getId (ct, ".");
	    ResolvedClass fqn = resolve (id1, ct.getParsePosition (), eh);
	    if (fqn == null && ct.size () > 1) {
		// ok, someone probably wrote something like "HashMap.Entry" or
		// "java.util.HashMap.Entry" which is somewhat problematic, but legal
		fqn = tryAllParts (ct, eh);
	    }
	    if (fqn == null) {
		eh.setIncomplete ();
		if (eh.mayReport ()) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), ct.getParsePosition (),
								 "Failed to find class: %s", id1));
		}
		return;
	    }
	    ct.setFullName (fqn.type);
	    ct.setTypeParameter (fqn.tp);
	    for (SimpleClassType sct : ct.get ()) {
		TypeArguments tas = sct.getTypeArguments ();
		if (tas != null)
		    tas.getTypeArguments ().forEach (tn -> setType (tn, eh));
	    }
	} else if (type instanceof ArrayType) {
	    ArrayType at = (ArrayType)type;
	    setType (at.getType (), eh);
	} else if (type instanceof Wildcard) {
	    Wildcard wc = (Wildcard)type;
	    WildcardBounds wb = wc.getBounds ();
	    if (wb != null)
		setType (wb.getClassType (), eh);
	} else {
	    eh.setIncomplete ();
	    if (eh.mayReport ()) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), type.getParsePosition (),
							     "Unhandled type: %s, %s",
							     type.getClass ().getName (), type));
	    }
	}
    }

    private String getId (ClassType ct, String join) {
	if (ct.size () == 1)
	    return ct.get ().get (0).getId ();
	return ct.get ().stream ().map (s -> s.getId ()).collect (Collectors.joining (join));
    }

    /** Try to find an outer class that has the inner classes for misdirected outer classes.
     */
    private ResolvedClass tryAllParts (ClassType ct, ErrorHandler eh) {
	StringBuilder sb = new StringBuilder ();
	List<SimpleClassType> scts = ct.get ();
	// group package names to class "java.util.HashMap"
	for (int i = 0, s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    if (i > 0)
		sb.append (".");
	    sb.append (sct.getId ());
	    ResolvedClass outerClass = resolve (sb.toString (), ct.getParsePosition (), eh);
	    if (outerClass != null) {
		// Ok, now check if Entry is an inner class either directly or in super class
		String fqn = checkForInnerClasses (scts, i + 1, outerClass.type, eh);
		if (fqn != null)
		    return new ResolvedClass (fqn);
	    }
	}
	return null;
    }

    private String checkForInnerClasses (List<SimpleClassType> scts, int i, String outerClass,
					 ErrorHandler eh) {
	String currentOuterClass = outerClass;
	for (int s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    String directInnerClass = currentOuterClass + "$" + sct.getId ();
	    if (hasVisibleType (directInnerClass)) {
		currentOuterClass = directInnerClass;
	    } else {
		currentOuterClass = checkSuperClasses (currentOuterClass, sct.getId (), eh);
		if (currentOuterClass == null)
		    return null;
	    }
	}
	return currentOuterClass;
    }

    private ResolvedClass resolve (String id, ParsePosition pos, ErrorHandler eh) {
	TypeParameter tp = getTypeParameter (id);
	if (tp != null) {
	    return new ResolvedClass (tp);
	}

	if (hasVisibleType (id))
	    return new ResolvedClass (id);

	String fqn = resolveInnerClass (id, eh);
	if (fqn == null)
	    fqn = resolveUsingImports (id, pos);
	if (fqn != null)
	    return new ResolvedClass (fqn);
	return null;
    }

    private static class ResolvedClass {
	public final String type;
	public final TypeParameter tp;

	public ResolvedClass (String type) {
	    this.type = type;
	    this.tp = null;
	}

	public ResolvedClass (TypeParameter tp) {
	    this.type = tp.getExpressionType ().getClassName ();
	    this.tp = tp;
	}
    }

    private String resolveInnerClass (String id, ErrorHandler eh) {
	// Check for inner class
	for (BodyPart ctn : containingTypes) {
	    String icn = ctn.fqn + "$" + id;
	    if (hasVisibleType (icn))
		return icn;
	}

	// Check for inner class of super classes
	for (BodyPart ctn : containingTypes) {
	    String fqn = checkSuperClasses (ctn.fqn, id, eh);
	    if (fqn != null)
		return fqn;
	}
	return null;
    }

    private String checkSuperClasses (String fullCtn, String id, ErrorHandler eh) {
	if (fullCtn == null)
	    return null;
	List<String> superclasses = getSuperClasses (fullCtn, eh);
	for (String superclass : superclasses) {
	    String icn = superclass + "$" + id;
	    if (hasVisibleType (icn))
		return icn;
	    String ssn = checkSuperClasses (superclass, id, eh);
	    if (ssn != null)
		return ssn;
	}
	return null;
    }

    private List<String> getSuperClasses (String type, ErrorHandler eh) {
	try {
	    Optional<List<String>> supers = cip.getSuperTypes (type, false);
	    return supers.isPresent () ? supers.get () : Collections.emptyList ();
	} catch (IOException e) {
	    eh.setIncomplete ();
	    if (eh.mayReport ())
		diagnostics.report (new NoSourceDiagnostics ("Failed to load class: " + type, e));
	}
	return Collections.emptyList ();
    }

    private String resolveUsingImports (String id, ParsePosition pos) {
	ImportHolder i = ih.stid.get (id);
	if (i != null && hasVisibleType (i.cas.name)) {
	    i.markUsed ();
	    return i.cas.name;
	}
	String fqn;
	fqn = tryPackagename (id, pos);
	if (fqn != null && hasVisibleType (fqn))
	    return fqn;
	fqn = trySingleStaticImport (id);
	if (fqn != null && hasVisibleType (fqn))
	    return fqn;
	fqn = tryTypeImportOnDemand (id, pos);
	if (fqn != null) // already checked cip.hasType
	    return fqn;
	fqn = tryStaticImportOnDemand (id);
	if (fqn != null) // already checked cip.hasType
	    return fqn;

	return null;
    }

    private TypeParameter getTypeParameter (String id) {
	TypeHolder th = currentTypes;
	while (th != null) {
	    TypeParameter tp = th.types.get (id);
	    if (tp != null)
		return tp;
	    th = th.parent;
	}
	return null;
    }

    private String tryPackagename (String id, ParsePosition pos) {
	if (packageName == null)
	    return null;
	return packageName.getDotName () + "." + id;
    }

    private String tryTypeImportOnDemand (String id, ParsePosition pos) {
	List<String> matches = new ArrayList<> ();
	for (ImportHolder ih : ih.tiod) {
	    String t = ih.cas.name + ih.cas.sep + id;
	    if (hasVisibleType (t)) {
		matches.add (t);
		ih.markUsed ();
	    }
	}
	if (matches.size () > 1) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), pos,
							 "Ambigous type: %s: %s", id, matches));
	}
	if (matches.isEmpty ())
	    return null;
	return matches.get (0);
    }

    private String trySingleStaticImport (String id) {
	ImportHandler.StaticImportType sit = ih.ssid.get (id);
	if (sit == null)
	    return null;
	sit.ssid.markUsed ();
	return sit.fullName;
    }

    private String tryStaticImportOnDemand (String id) {
	for (StaticImportOnDemandDeclaration siod : ih.siod) {
	    String fqn = siod.getName ().getDotName () + "$" + id;
	    if (hasVisibleType (fqn)) {
		siod.markUsed ();
		return fqn;
	    }
	}
	return null;
    }

    private class ImportHandler implements ImportVisitor {
	private final Map<String, ImportHolder> stid = new HashMap<> ();
	private final List<ImportHolder> tiod = new ArrayList<> ();
	private final Map<String, StaticImportType> ssid = new HashMap<> ();
	private final List<StaticImportOnDemandDeclaration> siod = new ArrayList<> ();

	public void visit (SingleTypeImportDeclaration i) {
	    DottedName dn = i.getName ();
	    ClassAndSeparator cas = getClassName (dn);
	    if (!hasVisibleType (cas.name)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getParsePosition (),
							     "Type not found: %s", cas.name));
		i.markUsed (); // Unused, but already flagged as bad, don't want multiple lines
	    }
	    ImportHolder prev = stid.put (dn.getLastPart (), new ImportHolder (i, cas));
	    if (prev != null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getParsePosition (),
							     "Name clash for: %s", dn.getLastPart ()));
	    }
	}

	public void visit (TypeImportOnDemandDeclaration i) {
	    tiod.add (new ImportHolder (i, getClassName (i.getName ())));
	}

	public void visit (SingleStaticImportDeclaration i) {
	    String fqn = i.getName ().getDotName ();
	    if (!hasVisibleType (fqn)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getParsePosition (),
							     "Type not found: %s", fqn));
		i.markUsed (); // Unused, but already flagged as bad, don't want multiple lines
		return;
	    }

	    ssid.put (i.getInnerId (), new StaticImportType (i));
	}

	public void visit (StaticImportOnDemandDeclaration i) {
	    siod.add (i);
	}

	private ClassAndSeparator getClassName (DottedName dn) {
	    StringBuilder sb = new StringBuilder ();
	    char sep = '.';
	    for (String c : dn.getParts ()) {
		if (sb.length () > 0)
		    sb.append (sep);
		sb.append (c);
		if (hasVisibleType (sb.toString ()))
		    sep = '$';
	    }
	    return new ClassAndSeparator (sb.toString (), sep);
	}

	private void addDefaultPackages () {
	    tiod.add (new ImportHolder (null, new ClassAndSeparator ("java.lang", '.')));
	}

	private class StaticImportType {
	    private final SingleStaticImportDeclaration ssid;
	    private final String containingClass;
	    private final String fullName;

	    public StaticImportType (SingleStaticImportDeclaration ssid) {
		this.ssid = ssid;
		containingClass = getClassName (ssid.getName ()).name;
		fullName = containingClass + "$" + ssid.getInnerId ();
	    }
	}
    }

    private boolean hasVisibleType (String fqn) {
	String topLevelClass = containingTypes.isEmpty () ? null : containingTypes.peekLast ().fqn;
	return hasVisibleType (fqn, topLevelClass);
    }

    private boolean hasVisibleType (String fqn, String topLevelClass) {
	LookupResult r = cip.hasVisibleType (fqn);
	if (!r.getFound ())
	    return false;
	if (FlagsHelper.isPublic (r.getAccessFlags ())) {
	    return true;
	} else if (FlagsHelper.isProtected (r.getAccessFlags ())) {
	    return samePackage (fqn, packageName) || insideSuperClass (fqn);
	} else if (FlagsHelper.isPrivate (r.getAccessFlags ())) {
	    return sameTopLevelClass (fqn, topLevelClass);
	}
	// No access level
	return samePackage (fqn, packageName);
    }

    private boolean samePackage (String fqn, DottedName pkg) {
	String start = pkg == null ? "" : pkg.getDotName ();
	return fqn.length () > start.length () && fqn.startsWith (start) &&
	    fqn.indexOf ('.', fqn.length ()) == -1;
    }

    private boolean insideSuperClass (String fqn) {
	for (BodyPart c : containingTypes)
	    if (insideSuperClass (fqn, c.fqn))
		return true;
	return false;
    }

    private boolean insideSuperClass (String fqn, String currentClass) {
	try {
	    Optional<List<String>> supers = cip.getSuperTypes (currentClass, false);
	    if (supers.isPresent ()) {
		for (String s :  supers.get ()) {
		    if (fqn.length () > s.length () && fqn.startsWith (s) &&
			fqn.charAt (s.length ()) == '$') {
			return true;
		    }
		    if (insideSuperClass (fqn, s))
			return true;
		}
	    }
	} catch (IOException e) {
	    return false;
	}
	return false;
    }

    private ClassType getClassType (String fqn, ParsePosition pos) {
	ClassType ct = new ClassType (null, pos);
	ct.setFullName (fqn);
	return ct;
    }

    private boolean sameTopLevelClass (String fqn, String topLevelClass) {
	return fqn.length () > topLevelClass.length () && fqn.startsWith (topLevelClass) &&
	    fqn.charAt (topLevelClass.length ()) == '$';
    }

    private static class ImportHolder {
	private final ImportDeclaration i;
	private final ClassAndSeparator cas;
	public ImportHolder (ImportDeclaration i, ClassAndSeparator cas) {
	    this.i = i;
	    this.cas = cas;
	}

	public void markUsed () {
	    if (i != null)
		i.markUsed ();
	}
    }

    private static class ClassAndSeparator {
	private final String name;
	private final char sep;

	public ClassAndSeparator (String name, char sep) {
	    this.name = name;
	    this.sep = sep;
	}
    }

    private static class StaticFieldAccess {
	private final String fqn;
	private final FieldInformation<?> field;

	public StaticFieldAccess (String fqn, FieldInformation<?> field) {
	    this.fqn = fqn;
	    this.field = field;
	}
    }

    private static class TypeHolder {
	private final TypeHolder parent;
	private final Map<String, TypeParameter> types;

	public TypeHolder (TypeHolder parent, Map<String, TypeParameter> types) {
	    this.parent = parent;
	    this.types = types;
	}
    }

    private interface ErrorHandler {
	boolean mayReport ();
	void setIncomplete ();
    }
}
