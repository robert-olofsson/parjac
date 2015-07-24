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
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.*;
import org.khelekore.parjac.tree.Result.TypeResult;

public class ClassSetter implements TreeVisitor {
    private final CompiledTypesHolder cth;
    private final ClassResourceHolder crh;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final DottedName packageName;
    private final ImportHandler ih = new ImportHandler ();
    private final Deque<String> containingTypeName = new ArrayDeque<> ();
    private final Deque<Set<String>> types = new ArrayDeque<> ();
    private boolean completed = true;

    public ClassSetter (CompiledTypesHolder cth, ClassResourceHolder crh,
			SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	this.cth = cth;
	this.crh = crh;
	this.tree = tree;
	this.diagnostics = diagnostics;

	CompilationUnit cu = tree.getCompilationUnit ();
	packageName = cu.getPackage ();
	ih.addDefaultPackages ();
	cu.getImports ().forEach (i -> i.visit (ih));
    }

    /**
     * @return true if every part was filled in correctly, false if something failed
     */
    public void fillIn () {
	tree.getCompilationUnit ().visit (this);
    }

    public boolean completed () {
	return completed;
    }

    @Override public boolean visit (NormalClassDeclaration c) {
	ClassType superclass = c.getSuperClass ();
	if (superclass != null)
	    setType (superclass);
	visitSuperInterfaces (c.getSuperInterfaces ());
	containingTypeName.push (cth.getFullName (c));
	registerTypeParameters (c.getTypeParameters ());
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	containingTypeName.push (cth.getFullName (e));
	visitSuperInterfaces (e.getSuperInterfaces ());
	registerTypeParameters (null);
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	containingTypeName.push (cth.getFullName (i));
	ExtendsInterfaces ei = i.getExtendsInterfaces ();
	if (ei != null)
	    visitSuperInterfaces (ei.get ());
	registerTypeParameters (i.getTypeParameters ());
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	containingTypeName.push (cth.getFullName (a));
	registerTypeParameters (null);
	return true;
    }

    @Override public boolean anonymousClass (ClassType ct, ClassBody b) {
	setType (ct);
	if (ct.getFullName () != null) {
	    containingTypeName.push (ct.getFullName ());
	    containingTypeName.push (cth.getFullName (b));
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

    @Override public void visit (FieldDeclaration f) {
	setType (f.getType ());
    }

    @Override public boolean visit (MethodDeclaration m) {
	registerTypeParameters (m.getTypeParameters ());
	Result r = m.getResult ();
	if (r instanceof Result.TypeResult)
	    setType (((TypeResult)r).get ());
	setTypes (m.getParameters ());
	return true;
    }

    @Override public void endMethod (MethodDeclaration m) {
	types.pop ();
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
	    String fqn = resolve (id1);
	    if (fqn == null && ct.size () > 1) {
		// ok, someone probably wrote something like "HashMap.Entry" or
		// "java.util.HashMap.Entry" which is somewhat problematic, but legal
		fqn = tryAllParts (ct);
	    }
	    if (fqn == null) {
		completed = false;
		if (diagnostics != null)
		    diagnostics.report (new SourceDiagnostics (tree.getOrigin (), ct.getParsePosition (),
							       "Failed to find class: " + id1));
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
	    if (diagnostics != null) {
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), type.getParsePosition (),
							   "Unhandled type: " + type.getClass ().getName () +
							   ", " + type));
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
	    String outerClass = resolve (sb.toString ());
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
	    if (validFullName (directInnerClass)) {
		currentOuterClass = directInnerClass;
	    } else {
		currentOuterClass = checkSuperClasses (currentOuterClass, sct.getId ());
		if (currentOuterClass == null)
		    return null;
	    }
	}
	return currentOuterClass;
    }

    private String resolve (String id) {
	if (isTypeParameter (id)) {
	    // TODO: how to handle this?
	    return "generic type: " + id;
	}

	if (validFullName (id))
	    return id;

	// check for inner class
	for (String ctn : containingTypeName) {
	    String icn = ctn + "$" + id;
	    if (validFullName (icn))
		return icn;
	}

	// check for inner class of super classes
	for (String ctn : containingTypeName) {
	    String fqn = checkSuperClasses (ctn, id);
	    if (fqn != null)
		return fqn;
	}

	return resolveUsingImports (id);
    }

    private String checkSuperClasses (String fullCtn, String id) {
	List<String> superclasses = getSuperClasses (fullCtn);
	for (String superclass : superclasses) {
	    String icn = superclass + "$" + id;
	    if (validFullName (icn))
		return icn;
	    String ssn = checkSuperClasses (superclass, id);
	    if (ssn != null)
		return ssn;
	}
	return null;
    }

    private List<String> getSuperClasses (String type) {
	Optional<List<String>> supers = cth.getSuperTypes (type);
	if (supers.isPresent ())
	    return supers.get ();
	try {
	    return crh.getSuperTypes (type);
	} catch (IOException e) {
	    completed = false;
	    if (diagnostics != null)
		diagnostics.report (new NoSourceDiagnostics ("Failed to load class: " + type, e));
	}
	return Collections.emptyList ();
    }

    private String resolveUsingImports (String id) {
	String fqn = ih.stid.get (id);
	if (fqn != null && validFullName (fqn))
	    return fqn;
	fqn = tryTypeImportOnDemand (id);
	if (fqn != null && validFullName (fqn))
	    return fqn;
	fqn = trySingleStaticImport (id);
	if (fqn != null && validFullName (fqn))
	    return fqn;
	fqn = tryStaticImportOnDemand (id);
	if (fqn != null && validFullName (fqn))
	    return fqn;

	return null;
    }

    private boolean isTypeParameter (String id) {
	for (Set<String> names : types)
	    if (names.contains (id))
		return true;
	return false;
    }

    private String tryTypeImportOnDemand (String id) {
	for (ClassAndSeparator cas : ih.tiod) {
	    String fqn = cas.name + cas.sep + id;
	    if (validFullName (fqn))
		return fqn;
	}
	return null;
    }

    private String trySingleStaticImport (String id) {
	return ih.ssid.get (id);
    }

    private String tryStaticImportOnDemand (String id) {
	for (StaticImportOnDemandDeclaration siod : ih.siod) {
	    String fqn = siod.getName ().getDotName () + "$" + id;
	    if (validFullName (fqn))
		return fqn;
	}
	return null;
    }

    private boolean validFullName (String fqn) {
	if (cth.getType (fqn) != null)
	    return true;

	if (crh.hasType (fqn))
	    return true;

	return false;
    }

    private class ImportHandler implements ImportVisitor {
	private final Map<String, String> stid = new HashMap<> ();
	private final List<ClassAndSeparator> tiod = new ArrayList<> ();
	private final Map<String, String> ssid = new HashMap<> ();
	private final List<StaticImportOnDemandDeclaration> siod = new ArrayList<> ();

	public void visit (SingleTypeImportDeclaration i) {
	    DottedName dn = i.getName ();
	    stid.put (dn.getLastPart (), getClassName (dn).name);
	}

	public void visit (TypeImportOnDemandDeclaration i) {
	    tiod.add (getClassName (i.getName ()));
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
		if (validFullName (sb.toString ()))
		    sep = '$';
	    }
	    return new ClassAndSeparator (sb.toString (), sep);
	}

	private void addDefaultPackages () {
	    if (packageName != null)
		tiod.add (getClassName (packageName));
	    tiod.add (new ClassAndSeparator ("java.lang", '.'));
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
