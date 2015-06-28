package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public ClassSetter (CompiledTypesHolder cth, ClassResourceHolder crh,
			SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	this.cth = cth;
	this.crh = crh;
	this.tree = tree;
	this.diagnostics = diagnostics;

	CompilationUnit cu = tree.getCompilationUnit ();
	packageName = cu.getPackage ();
	cu.getImports ().forEach (i -> i.visit (ih));
	ih.addDefaultPackages ();
    }

    public void fillIn () {
	tree.getCompilationUnit ().visit (this);
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
	// System.err.println ("Trying to set type for: " + type);
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
	    String id1 = getId (ct, ".");
	    String fqn = resolve (id1);
	    // System.err.println ("id1: " + id1 + ", fqn: " + fqn);
	    if (fqn == null && ct.size () > 1) {
		String firstName = ct.get ().get (0).getId ();
		String outerName = ih.stid.get (firstName);
		if (outerName != null) {
		    List<SimpleClassType> ls = ct.get ();
		    fqn = outerName + "." +
			ls.subList (1, ls.size ()).stream ().
			map (s -> s.getId ()).collect (Collectors.joining ("."));
		    if (!validFullName (fqn))
			fqn = null;
		}
	    }
	    if (fqn == null && diagnostics != null)
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), ct.getParsePosition (),
							   "Failed to find class: " + id1));
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
	    if (diagnostics != null)
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), type.getParsePosition (),
							   "Unhandled type: " + type.getClass ().getName () +
							   ", " + type));
	}
    }

    private String getId (ClassType ct, String join) {
	return ct.get ().stream ().map (s -> s.getId ()).collect (Collectors.joining (join));
    }

    private String resolve (String id) {
	// System.err.println ("Trying to resolve: " + id);
	TreeNode type = cth.getType (id);
	if (type != null)
	    return id;

	if (isTypeParameter (id)) {
	    return "generic type: " + id;
	}

	if (crh.hasType (id)) {
	    return id;
	}

	// check for inner class
	for (String ctn : containingTypeName) {
	    String icn = ctn + "." + id;
	    // System.err.println ("icn: " + icn + " => " + type);
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
	// System.err.println ("fullCtn: " + fullCtn + ", id: " + id + ", superclasses: " + superclasses);
	for (String superclass : superclasses) {
	    String icn = superclass + "." + id;
	    // System.err.println ("sicn: " + icn + " => " + validFullName(icn));
	    if (validFullName (icn))
		return icn;
	    String ssn = checkSuperClasses (superclass, id);
	    if (ssn != null)
		return ssn;
	}
	return null;
    }

    private List<String> getSuperClasses (String type) {
	/* TODO: move this into cth */
	TreeNode tn = cth.getType (type);
	if (tn instanceof NormalClassDeclaration) {
	    NormalClassDeclaration ncd = (NormalClassDeclaration)tn;
	    List<String> ret = new ArrayList<> ();
	    ClassType ct = ncd.getSuperClass ();
	    if (ct != null) {
		if (ct.getFullName () == null)
		    return Collections.emptyList ();
		ret.add (ct.getFullName ());
	    }
	    InterfaceTypeList ifs = ncd.getSuperInterfaces ();
	    if (ifs != null)
		ifs.get ().forEach (ic -> ret.add (ic.getFullName ()));
	    return ret;
	} else if (tn instanceof NormalInterfaceDeclaration) {
	    NormalInterfaceDeclaration nid = (NormalInterfaceDeclaration)tn;
	    ExtendsInterfaces ei = nid.getExtendsInterfaces ();
	    if (ei != null) {
		List<ClassType> cts = ei.get ().get ();
		return cts.stream ().map (ct -> ct.getFullName ()).collect (Collectors.toList ());
	    }
	}
	try {
	    return crh.getSuperTypes (type);
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to load class: " + type, e));
	}
	return Collections.emptyList ();
    }

    private String resolveUsingImports (String id) {
	String fqn = ih.stid.get (id);
	if (fqn != null && validFullName (fqn))
	    return fqn;
	fqn = tryTypeImportOnDemand (id);
	if (fqn != null)
	    return fqn;
	fqn = trySingleStaticImport (id);
	if (fqn != null)
	    return fqn;
	fqn = tryStaticImportOnDemand (id);
	if (fqn != null)
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
	for (String p : ih.tiod) {
	    String fqn = p + "." + id;
	    if (validFullName (fqn))
		return fqn;
	}
	return null;
    }

    private String trySingleStaticImport (String id) {
	for (SingleStaticImportDeclaration ssid : ih.ssid) {
	    if (id.equals (ssid.getInnerId ()))
		return ssid.getFullName ();
	}
	return null;
    }

    private String tryStaticImportOnDemand (String id) {
	for (StaticImportOnDemandDeclaration siod : ih.siod) {
	    String fqn = siod.getName ().getDotName () + "." + id;
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
	private final List<String> tiod = new ArrayList<> ();
	private final List<SingleStaticImportDeclaration> ssid = new ArrayList<> ();
	private final List<StaticImportOnDemandDeclaration> siod = new ArrayList<> ();

	public void visit (SingleTypeImportDeclaration i) {
	    DottedName dn = i.getName ();
	    stid.put (dn.getLastPart (), dn.getDotName ());
	}

	public void visit (TypeImportOnDemandDeclaration i) {
	    tiod.add (i.getName ().getDotName ());
	}

	public void visit (SingleStaticImportDeclaration i) {
	    ssid.add (i);
	}

	public void visit (StaticImportOnDemandDeclaration i) {
	    siod.add (i);
	}

	private void addDefaultPackages () {
	    if (packageName != null)
		tiod.add (packageName.getDotName ());
	    tiod.add ("java.lang");
	}
    }
}
