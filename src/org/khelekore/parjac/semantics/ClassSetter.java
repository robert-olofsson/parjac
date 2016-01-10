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
import java.util.Set;
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
    // Type parameters, should probably be moved into scope instead
    private final Deque<Set<String>> types = new ArrayDeque<> ();
    private final Map<TreeNode, Scope> scopes = new HashMap<> ();

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
	classSetters.parallelStream ().forEach (cs -> cs.checkUnusedInport ());
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

    /* TODO: make sure that we replace all of these DottedName:s with correct things
     * PrimaryNoNewArray: (foo.bar.class, foo.Bar.this)
     * * ClassInstanceCreationExpression:  (foo.bar.new Baz())
     * * FieldAccess: (foo.bar.Baz.super.asdf)
     * * ArrayAccess:
     * * MethodInvocation:
     * MethodReference:   foo.Bar.super::<Whatever>methodName, MethodReference.SuperMethodReference
     * PostfixExpression:
     */
    public void addScopes (Queue<ClassSetter> rest, int level) {
	ScopeSetter ss = new ScopeSetter (level);
	tree.getCompilationUnit ().visit (ss);
	if (!ss.completed ())
	    rest.add (this);
    }

    public void checkUnusedInport () {
	List<ImportDeclaration> imports = tree.getCompilationUnit ().getImports ();
	imports.stream ().filter (i -> !i.hasBeenUsed ()).forEach (i -> checkUnusedImport (i));
    }

    private void checkUnusedImport (ImportDeclaration i) {
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

	public boolean mayReport () {
	    return level > 0;
	}

	public void setIncomplete () {
	    completed = false;
	}

	public boolean completed () {
	    return completed;
	}

	@Override public boolean visit (NormalClassDeclaration c) {
	    ClassType superclass = c.getSuperClass ();
	    if (superclass != null)
		setType (superclass, this);
	    visitSuperInterfaces (c.getSuperInterfaces (), this);
	    String fqn = cip.getFullName (c);
	    containingTypes.push (new BodyPart (fqn, c.getBody ()));
	    registerTypeParameters (c.getTypeParameters (), this);
	    addScopeAndFields (fqn, c, c.getBody ());
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    String fqn = cip.getFullName (e);
	    containingTypes.push (new BodyPart (fqn, e.getBody ()));
	    visitSuperInterfaces (e.getSuperInterfaces (), this);
	    registerTypeParameters (null, this);
	    addScopeAndFields (fqn, e, e.getBody ());
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    String fqn = cip.getFullName (i);
	    containingTypes.push (new BodyPart (fqn, i.getBody ()));
	    ExtendsInterfaces ei = i.getExtendsInterfaces ();
	    if (ei != null)
		visitSuperInterfaces (ei.get (), this);
	    registerTypeParameters (i.getTypeParameters (), this);
	    addScopeAndFields (fqn, i, i.getBody ());
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    String fqn = cip.getFullName (a);
	    containingTypes.push (new BodyPart (fqn, a.getBody ()));
	    registerTypeParameters (null, this);
	    addScopeAndFields (fqn, a, a.getBody ());
	    return true;
	}

	@Override public void endType () {
	    registerMethods (containingTypes.pop ());
	    types.pop ();
	    currentScope = currentScope.endScope ();
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    addScope (b, Scope.Type.CLASS);
	    if (ct.getFullName () != null) {
		containingTypes.push (new BodyPart (ct.getFullName (), null));
		String fqn = cip.getFullName (b);
		containingTypes.push (new BodyPart (fqn, b));
		registerTypeParameters (null, this);
		addFields (fqn, b, currentScope);
		return true;
	    }
	    // No need to continue here
	    // TODO: do we need to produce errors or warnings?
	    return false;
	}

	@Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	    if (ct.getFullName () != null) {
		types.pop ();
		registerMethods (containingTypes.pop ());
		containingTypes.pop ();
	    }
	    currentScope = currentScope.endScope ();
	}

	// TODO: handle InstanceInitializer and StaticInitializer, they need scope

	@Override public boolean visit (ConstructorDeclaration c) {
	    registerTypeParameters (c.getTypeParameters (), this);
	    setTypes (c.getParameters (), this);
	    addScopeAndParameters (c, c.getParameters ());
	    // TODO: store method information
	    return true;
	}

	@Override public void endConstructor (ConstructorDeclaration c) {
	    currentScope = currentScope.endScope ();
	}

	@Override public boolean visit (MethodDeclaration m) {
	    registerTypeParameters (m.getTypeParameters (), this);
	    Result r = m.getResult ();
	    if (r instanceof Result.TypeResult)
		setType (r.getReturnType (), this);
	    setTypes (m.getParameters (), this);

	    addScopeAndParameters (m, m.getParameters ());
	    return true;
	}

	@Override public void endMethod (MethodDeclaration m) {
	    currentScope = currentScope.endScope ();
	}

	@Override public boolean visit (Block b) {
	    addScope (b, Scope.Type.LOCAL);
	    return true;
	}

	@Override public void endBlock () {
	    currentScope = currentScope.endScope ();
	}

	// Need to set the expression type before we can replace things
	@Override public void endReturn (ReturnStatement r) {
	    if (r.hasExpression ())
		r.setExpression (replaceAndSetType (r.getExpression ()));
	}

	@Override public void visit (Assignment a) {
	    TreeNode lhs = a.lhs ();
	    a.lhs (replaceAndSetType (lhs));
	}

	@Override public void visit (ClassInstanceCreationExpression c) {
	    TreeNode from = c.getFrom ();
	    if (from == null) {
		setType (c.getId (), this);
	    } else {
		c.setFrom (replaceAndSetType (from));
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
	}

	@Override public boolean visit (MethodInvocation m) {
	    TreeNode on = m.getOn ();
	    if (on != null)
		m.setOn (replaceAndSetType (on));
	    ArgumentList al = m.getArgumentList ();
	    if (al != null) {
		List<TreeNode> ls = al.get ();
		for (int i = 0, s = ls.size (); i < s; i++)
		    ls.set (i, replaceAndSetType (ls.get (i)));
	    }
	    return true;
	}

	@Override public boolean visit (LocalVariableDeclaration l) {
	    setType (l.getType (), this);
	    for (VariableDeclarator v : l.getVariables ().get ()) {
		TreeNode tn = v.getInitializer ();
		if (tn != null)
		    tn.visit (this);
		currentScope.tryToAdd (l, v, tree, diagnostics);
	    }
	    return false;
	}

	@Override public boolean visit (CastExpression c) {
	    setType (c.getType (), this);
	    return true;
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
		    FieldInformation<?> fi = scope.find (p, scope.isStatic ());
		    if (fi != null) {
			current = new Identifier (p, pos, fi.getExpressionType ());
			continue;
		    }
		} else {
		    sb.append (".");
		}
		sb.append (p);
		String outerClass = ct == null ? resolve (sb.toString (), pos, eh) : null;
		if (outerClass != null) {
		    DottedName pdn = new DottedName (parts.subList (0, i + 1), pos);
		    ct = pdn.toClassType ();
		    ct.setFullName (outerClass);
		    current = ct;
		    continue;
		}
		if (current != null)
		    current = new FieldAccess (current, p, pos);
		else
		    current = new Identifier (p, pos);
	    }
	    return current;
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

	private TreeNode replaceAndSetType (TreeNode tn) {
	    if (tn.getExpressionType () == null && tn instanceof Identifier) {
		Identifier i = (Identifier)tn;
		FieldInformation<?> fi = currentScope.find (i.get (), currentScope.isStatic ());
		if (fi != null) {
		    tn = new Identifier (i.get (), i.getParsePosition (), fi.getExpressionType ());
		}
	    }
	    return tn;
	}

	private void addScopeAndFields (String fqn, FlaggedType ft, TreeNode part) {
	    addScope (ft, Scope.Type.CLASS);
	    addFields (fqn, part, currentScope);
	}

	private void addScopeAndParameters (FlaggedType ft, FormalParameterList fpl) {
	    addScope (ft, Scope.Type.LOCAL);
	    if (fpl != null) {
		NormalFormalParameterList pl = fpl.getParameters ();
		if (pl != null) {
		    List<FormalParameter> ls = pl.getFormalParameters ();
		    if (ls != null)
			ls.forEach (fp -> currentScope.tryToAdd (fp, tree, diagnostics));
		}
		LastFormalParameter lfp = pl.getLastFormalParameter ();
		if (lfp != null) {
		    currentScope.tryToAdd (lfp, tree, diagnostics);
		}
	    }
	}

	private void addScope (FlaggedType ft, Scope.Type type) {
	    currentScope = new Scope (currentScope, type, FlagsHelper.isStatic (ft.getFlags ()));
	    scopes.put (ft, currentScope);
	}

	private void addScope (TreeNode tn, Scope.Type type) {
	    currentScope = new Scope (currentScope, type, currentScope.isStatic ());
	    scopes.put (tn, currentScope);
	}

	private void addFields (String fqn, TreeNode tn, Scope scope) {
	    tn.visit (new FieldRegistrator (scope));
	    cip.registerFields (fqn, scope.getVariables ());
	}

	private class FieldRegistrator extends SiblingVisitor {
	    private final Scope scope;

	    public FieldRegistrator (Scope scope) {
		this.scope = scope;
	    }

	    @Override public void visit (FieldDeclaration f) {
		setType (f.getType (), ScopeSetter.this);
		f.getVariables ().get ().forEach (v -> scope.tryToAdd (f, v, tree, diagnostics));
	    }

	    @Override public boolean visit (EnumConstant c) {
		c.setType (containingTypes.peek ().fqn);
		scope.tryToAdd (c, tree, diagnostics);
		return false;
	    }
	}

	private void registerMethods (BodyPart bp) {
	    MethodRegistrator mr = new MethodRegistrator ();
	    bp.body.visit (mr);
	    cip.registerMethods (bp.fqn, mr.methods);
	}

	private class MethodRegistrator extends SiblingVisitor {
	    Map<String, List<MethodInformation>> methods = new HashMap<> ();
	    @Override public boolean visit (MethodDeclaration m) {
		String name = m.getMethodName ();
		List<MethodInformation> ls = methods.get (name);
		if (ls == null) {
		    ls = new ArrayList<> ();
		    methods.put (name, ls);
		}
		ls.add (getMethodInformation (m));
		return false;
	    }

	    private MethodInformation getMethodInformation (MethodDeclaration m) {
		return new MethodInformation (m.getFlags (),
					      m.getMethodName (),
					      m.getDescription (),
					      null,   // TODO: fill in
					      EMTPY); // TODO: fill in
	    }
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

    private void registerTypeParameters (TypeParameters tps, ErrorHandler eh) {
	if (tps != null) {
	    types.push (tps.get ().stream ().map (t -> t.getId ()).collect (Collectors.toSet ()));
	    tps.get ().forEach (b -> checkClasses (b, eh));
	} else {
	    types.push (Collections.emptySet ());
	}
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
	    String fqn = resolve (id1, ct.getParsePosition (), eh);
	    if (fqn == null && ct.size () > 1) {
		// ok, someone probably wrote something like "HashMap.Entry" or
		// "java.util.HashMap.Entry" which is somewhat problematic, but legal
		fqn = tryAllParts (ct, eh);
	    }
	    if (fqn == null) {
		eh.setIncomplete ();
		if (eh.mayReport ())
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), ct.getParsePosition (),
								 "Failed to find class: %s", id1));
	    }
	    ct.setFullName (fqn);
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
    private String tryAllParts (ClassType ct, ErrorHandler eh) {
	StringBuilder sb = new StringBuilder ();
	List<SimpleClassType> scts = ct.get ();
	// group package names to class "java.util.HashMap"
	for (int i = 0, s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    if (i > 0)
		sb.append (".");
	    sb.append (sct.getId ());
	    String outerClass = resolve (sb.toString (), ct.getParsePosition (), eh);
	    if (outerClass != null) {
		// Ok, now check if Entry is an inner class either directly or in super class
		String fqn = checkForInnerClasses (scts, i + 1, outerClass, eh);
		if (fqn != null)
		    return fqn;
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

    private String resolve (String id, ParsePosition pos, ErrorHandler eh) {
	if (isTypeParameter (id)) {
	    // TODO: how to handle this?
	    return "generic type: " + id;
	}

	if (hasVisibleType (id))
	    return id;

	String fqn = resolveInnerClass (id, eh);
	if (fqn != null)
	    return fqn;

	return resolveUsingImports (id, pos);
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
	    Optional<List<String>> supers = cip.getSuperTypes (type);
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

    private boolean isTypeParameter (String id) {
	for (Set<String> names : types)
	    if (names.contains (id))
		return true;
	return false;
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
	return ih.ssid.get (id);
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
	private final Map<String, String> ssid = new HashMap<> ();
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
	    ssid.put (i.getInnerId (), getClassName (i.getName ()).name + "$" + i.getInnerId ());
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
	    Optional<List<String>> supers = cip.getSuperTypes (currentClass);
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

    private interface ErrorHandler {
	boolean mayReport ();
	void setIncomplete ();
    }
}
