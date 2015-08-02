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

public class ClassSetter implements TreeVisitor {
    private final ClassInformationProvider cip;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final DottedName packageName;
    private final ImportHandler ih = new ImportHandler ();
    private final Deque<String> containingTypeName = new ArrayDeque<> ();
    private final Deque<Set<String>> types = new ArrayDeque<> ();
    private final int level;
    private boolean completed = true;

    /* TODO: make sure that we replace all of these DottedName:s with correct things
      PrimaryNoNewArray: (foo.bar.class, foo.bar.this)
      * ClassInstanceCreationExpression:  (foo.bar.new Baz())
      * FieldAccess: (foo.bar.Baz.super.asdf)
      * ArrayAccess:
      * MethodInvocation:
      MethodReference:   foo.Bar.super::<Whatever>methodName, MethodReference.SuperMethodReference
      PostfixExpression:
    */

    public static void fillInClasses (ClassInformationProvider cip,
				      List<SyntaxTree> trees,
				      CompilerDiagnosticCollector diagnostics) {
	// Fill in correct classes
	// Depending on order we may not have correct parents on first try.
	// We collect the trees that fails and tries again
	Queue<SyntaxTree> rest = new ConcurrentLinkedQueue<SyntaxTree> ();
	trees.parallelStream ().forEach (t -> fillInClasses (cip, t, diagnostics, rest, 0));
	if (!rest.isEmpty ()) {
	    Queue<SyntaxTree> rest2 = new ConcurrentLinkedQueue<SyntaxTree> ();
	    rest.parallelStream ().forEach (t -> fillInClasses (cip, t, diagnostics, rest2, 1));
	}
	trees.parallelStream().forEach (t -> checkUnusedInport (t, diagnostics));
    }

    private static void fillInClasses (ClassInformationProvider cip,
				       SyntaxTree tree,
				       CompilerDiagnosticCollector diagnostics,
				       Queue<SyntaxTree> rest, int level) {
	ClassSetter cs = new ClassSetter (cip, tree, diagnostics, level);
	cs.fillIn ();
	if (!cs.completed ())
	    rest.add (tree);
    }

    private static void checkUnusedInport (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	List<ImportDeclaration> imports = tree.getCompilationUnit ().getImports ();
	imports.stream ().filter (i -> !i.hasBeenUsed ())
	    .forEach (i -> checkUnusedImport (tree, i, diagnostics));
    }

    private static void checkUnusedImport (SyntaxTree tree, ImportDeclaration i,
					   CompilerDiagnosticCollector diagnostics) {
	diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (), i.getParsePosition (),
						       "Unused import"));
    }

    public ClassSetter (ClassInformationProvider cip, SyntaxTree tree,
			CompilerDiagnosticCollector diagnostics, int level) {
	this.cip = cip;
	this.tree = tree;
	this.diagnostics = diagnostics;
	this.level = level;

	CompilationUnit cu = tree.getCompilationUnit ();
	packageName = cu.getPackage ();
	ih.addDefaultPackages ();
	cu.getImports ().forEach (i -> i.visit (ih));
    }

    public void fillIn () {
	tree.getCompilationUnit ().visit (this);
    }

    private boolean completed () {
	return completed;
    }

    @Override public boolean visit (NormalClassDeclaration c) {
	ClassType superclass = c.getSuperClass ();
	if (superclass != null)
	    setType (superclass);
	visitSuperInterfaces (c.getSuperInterfaces ());
	containingTypeName.push (cip.getFullName (c));
	registerTypeParameters (c.getTypeParameters ());
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	containingTypeName.push (cip.getFullName (e));
	visitSuperInterfaces (e.getSuperInterfaces ());
	registerTypeParameters (null);
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	containingTypeName.push (cip.getFullName (i));
	ExtendsInterfaces ei = i.getExtendsInterfaces ();
	if (ei != null)
	    visitSuperInterfaces (ei.get ());
	registerTypeParameters (i.getTypeParameters ());
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	containingTypeName.push (cip.getFullName (a));
	registerTypeParameters (null);
	return true;
    }

    @Override public boolean anonymousClass (ClassType ct, ClassBody b) {
	setType (ct);
	if (ct.getFullName () != null) {
	    containingTypeName.push (ct.getFullName ());
	    containingTypeName.push (cip.getFullName (b));
	    registerTypeParameters (null);
	    return true;
	}
	return false;
    }

    @Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	if (ct.getFullName () != null) {
	    endType ();
	    containingTypeName.pop ();
	}
    }

    @Override public void endType () {
	containingTypeName.pop ();
	types.pop ();
    }

    private void visitSuperInterfaces (InterfaceTypeList superInterfaces) {
	if (superInterfaces != null)
	    setTypes (superInterfaces);
    }

    @Override public boolean visit (ConstructorDeclaration c) {
	registerTypeParameters (c.getTypeParameters ());
	setTypes (c.getParameters ());
	return true;
    }

    @Override public void endConstructor (ConstructorDeclaration c) {
	types.pop ();
    }

    @Override public void visit (ExplicitConstructorInvocation eci) {
	TreeNode type = eci.getType ();
	if (type instanceof DottedName) {
	    eci.setType (replaceWithChainedFieldAccess ((DottedName)type));
	}
    }

    @Override public void visit (FieldDeclaration f) {
	setType (f.getType ());
    }

    @Override public boolean visit (MethodDeclaration m) {
	registerTypeParameters (m.getTypeParameters ());
	Result r = m.getResult ();
	if (r instanceof Result.TypeResult)
	    setType (r.getReturnType ());
	setTypes (m.getParameters ());
	return true;
    }

    @Override public void endMethod (MethodDeclaration m) {
	types.pop ();
    }

    @Override public void visit (Assignment a) {
	TreeNode lhs = a.lhs ();
	if (lhs instanceof DottedName) {
	    a.lhs (replaceWithChainedFieldAccess ((DottedName)lhs));
	}
    }

    @Override public void visit (ClassInstanceCreationExpression c) {
	TreeNode from = c.getFrom ();
	if (from instanceof DottedName) {
	    c.setFrom (replaceWithChainedFieldAccess((DottedName)from));
	}
    }

    @Override public void visit (UntypedClassInstanceCreationExpression c) {
	setType (c.getId ());
    }

    @Override public boolean visit (MethodInvocation m) {
	TreeNode on = m.getOn ();
	if (on instanceof DottedName) {
	    on = replaceWithChainedFieldAccess ((DottedName)on);
	    m.setOn (on);
	}
	if (on instanceof ClassType) {
	    setType ((ClassType)on);
	}
	return true;
    }

    @Override public boolean visit (ArrayAccess a) {
	TreeNode from = a.getFrom ();
	if (from instanceof DottedName) {
	    a.setFrom (replaceWithChainedFieldAccess((DottedName)from));
	}
	return true;
    }

    private TreeNode replaceWithChainedFieldAccess (DottedName dn) {
	TreeNode current = null;
	final ParsePosition pos = dn.getParsePosition ();
	StringBuilder sb = new StringBuilder ();
	ClassType ct = null;
	List<String> parts = dn.getParts ();
	for (int i = 0, s = parts.size (); i < s; i++) {
	    String p = parts.get (i);
	    if (i > 0)
		sb.append (".");
	    sb.append (p);
	    String outerClass = ct == null ? resolve (sb.toString (), pos) : null;
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

    private void registerTypeParameters (TypeParameters tps) {
	if (tps != null) {
	    types.push (tps.get ().stream ().map (t -> t.getId ()).collect (Collectors.toSet ()));
	} else {
	    types.push (Collections.emptySet ());
	}
    }

    private void setTypes (InterfaceTypeList ls) {
	if (ls == null)
	    return;
	for (ClassType ct : ls.get ())
	    setType (ct);
    }

    private void setTypes (FormalParameterList ls) {
	if (ls == null)
	    return;

	NormalFormalParameterList fps = ls.getParameters ();
	List<FormalParameter> args = fps.getFormalParameters ();
	if (args != null) {
	    for (FormalParameter fp : args)
		setType (fp.getType ());
	}
	LastFormalParameter lfp = fps.getLastFormalParameter ();
	if (lfp != null) {
	    setType (lfp.getType ());
	}
    }

    private void setType (TreeNode type) {
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
	    String fqn = resolve (id1, ct.getParsePosition ());
	    if (fqn == null && ct.size () > 1) {
		// ok, someone probably wrote something like "HashMap.Entry" or
		// "java.util.HashMap.Entry" which is somewhat problematic, but legal
		fqn = tryAllParts (ct);
	    }
	    if (fqn == null) {
		completed = false;
		if (mayReport ())
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), ct.getParsePosition (),
								 "Failed to find class: %s", id1));
	    }
	    ct.setFullName (fqn);
	    for (SimpleClassType sct : ct.get ()) {
		TypeArguments tas = sct.getTypeArguments ();
		if (tas != null)
		    tas.getTypeArguments ().forEach (tn -> setType (tn));
	    }
	} else if (type instanceof ArrayType) {
	    ArrayType at = (ArrayType)type;
	    setType (at.getType ());
	} else if (type instanceof Wildcard) {
	    Wildcard wc = (Wildcard)type;
	    WildcardBounds wb = wc.getBounds ();
	    if (wb != null)
		setType (wb.getClassType ());
	} else {
	    completed = false;
	    if (mayReport ()) {
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
    private String tryAllParts (ClassType ct) {
	StringBuilder sb = new StringBuilder ();
	List<SimpleClassType> scts = ct.get ();
	// group package names to class "java.util.HashMap"
	for (int i = 0, s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    if (i > 0)
		sb.append (".");
	    sb.append (sct.getId ());
	    String outerClass = resolve (sb.toString (), ct.getParsePosition ());
	    if (outerClass != null) {
		// Ok, now check if Entry is an inner class either directly or in super class
		String fqn = checkForInnerClasses (scts, i + 1, outerClass);
		if (fqn != null)
		    return fqn;
	    }
	}
	return null;
    }

    private String checkForInnerClasses (List<SimpleClassType> scts, int i, String outerClass) {
	String currentOuterClass = outerClass;
	for (int s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    String directInnerClass = currentOuterClass + "$" + sct.getId ();
	    if (cip.hasType (directInnerClass)) {
		currentOuterClass = directInnerClass;
	    } else {
		currentOuterClass = checkSuperClasses (currentOuterClass, sct.getId ());
		if (currentOuterClass == null)
		    return null;
	    }
	}
	return currentOuterClass;
    }

    private String resolve (String id, ParsePosition pos) {
	if (isTypeParameter (id)) {
	    // TODO: how to handle this?
	    return "generic type: " + id;
	}

	if (cip.hasType (id))
	    return id;

	// check for inner class
	for (String ctn : containingTypeName) {
	    String icn = ctn + "$" + id;
	    if (cip.hasType (icn))
		return icn;
	}

	// check for inner class of super classes
	for (String ctn : containingTypeName) {
	    String fqn = checkSuperClasses (ctn, id);
	    if (fqn != null)
		return fqn;
	}

	return resolveUsingImports (id, pos);
    }

    private String checkSuperClasses (String fullCtn, String id) {
	List<String> superclasses = getSuperClasses (fullCtn);
	for (String superclass : superclasses) {
	    String icn = superclass + "$" + id;
	    if (cip.hasType (icn))
		return icn;
	    String ssn = checkSuperClasses (superclass, id);
	    if (ssn != null)
		return ssn;
	}
	return null;
    }

    private List<String> getSuperClasses (String type) {
	try {
	    Optional<List<String>> supers = cip.getSuperTypes (type);
	    return supers.isPresent () ? supers.get () : Collections.emptyList ();
	} catch (IOException e) {
	    completed = false;
	    if (mayReport ())
		diagnostics.report (new NoSourceDiagnostics ("Failed to load class: " + type, e));
	}
	return Collections.emptyList ();
    }

    private String resolveUsingImports (String id, ParsePosition pos) {
	ImportHolder i = ih.stid.get (id);
	if (i != null && cip.hasType (i.cas.name)) {
	    i.markUsed ();
	    return i.cas.name;
	}
	String fqn;
	fqn = tryPackagename (id, pos);
	if (fqn != null && cip.hasType (fqn))
	    return fqn;
	fqn = tryTypeImportOnDemand (id, pos);
	if (fqn != null) // already checked cip.hasType
	    return fqn;
	fqn = trySingleStaticImport (id);
	if (fqn != null && cip.hasType (fqn))
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
	    if (cip.hasType (t)) {
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
	    if (cip.hasType (fqn)) {
		siod.markUsed ();
		return fqn;
	    }
	}
	return null;
    }

    private boolean mayReport () {
	return level > 0;
    }

    private class ImportHandler implements ImportVisitor {
	private final Map<String, ImportHolder> stid = new HashMap<> ();
	private final List<ImportHolder> tiod = new ArrayList<> ();
	private final Map<String, String> ssid = new HashMap<> ();
	private final List<StaticImportOnDemandDeclaration> siod = new ArrayList<> ();

	public void visit (SingleTypeImportDeclaration i) {
	    DottedName dn = i.getName ();
	    ClassAndSeparator cas = getClassName (dn);
	    if (!cip.hasType (cas.name) && level == 0) {
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
		if (cip.hasType (sb.toString ()))
		    sep = '$';
	    }
	    return new ClassAndSeparator (sb.toString (), sep);
	}

	private void addDefaultPackages () {
	    tiod.add (new ImportHolder (null, new ClassAndSeparator ("java.lang", '.')));
	}
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
}
