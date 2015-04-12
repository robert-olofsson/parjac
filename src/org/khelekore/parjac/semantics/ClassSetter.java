package org.khelekore.parjac.semantics;

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

	// System.err.println ("compiled types: " + cth.getTypes ());
	CompilationUnit cu = tree.getCompilationUnit ();
	packageName = cu.getPackage ();
	cu.getImports ().forEach (i -> i.visit (ih));
	ih.addDefaultPackages ();
    }

    public void fillIn () {
	tree.getCompilationUnit ().visit (this);
    }

    @Override public void visit (NormalClassDeclaration c) {
	ClassType superclass = c.getSuperClass ();
	if (superclass != null)
	    setType (superclass);
	visitSuperInterfaces (c.getSuperInterfaces ());
	containingTypeName.push (cth.getId (c));
	registerTypeParameters (c.getTypeParameters ());
    }

    @Override public void visit (EnumDeclaration e) {
	containingTypeName.push (cth.getId (e));
	visitSuperInterfaces (e.getSuperInterfaces ());
	registerTypeParameters (null);
    }

    @Override public void visit (NormalInterfaceDeclaration i) {
	containingTypeName.push (cth.getId (i));
	ExtendsInterfaces ei = i.getExtendsInterfaces ();
	if (ei != null)
	    visitSuperInterfaces (ei.get ());
	registerTypeParameters (i.getTypeParameters ());
    }

    @Override public void visit (AnnotationTypeDeclaration a) {
	containingTypeName.push (cth.getId (a));
	registerTypeParameters (null);
    }

    @Override public void anonymousClass (ClassBody b) {
	containingTypeName.push (cth.getId (b));
	registerTypeParameters (null);
    }

    @Override public void endType () {
	containingTypeName.pop ();
	types.pop ();
    }

    private void visitSuperInterfaces (InterfaceTypeList superInterfaces) {
	if (superInterfaces != null)
	    setTypes (superInterfaces);
    }

    @Override public void visit (ConstructorDeclaration c) {
	setTypes (c.getParameters ());
    }

    @Override public void visit (FieldDeclaration f) {
	setType (f.getType ());
    }

    @Override public void visit (MethodDeclaration m) {
	registerTypeParameters (m.getTypeParameters ());
	Result r = m.getResult ();
	if (r instanceof Result.TypeResult)
	    setType (((TypeResult)r).get ());
	setTypes (m.getParameters ());
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
	if (type instanceof ClassType) {
	    ClassType ct = (ClassType)type;
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
	    if (fqn == null)
		diagnostics.report (new SourceDiagnostics (tree.getOrigin (), ct.getParsePosition (),
							   "Failed to find class: " + id1));
	    ct.setFullName (fqn);
	} else {
	    System.err.println ("Unhandled type: " + type.getClass ().getName () + ", " + type);
	}
	// TODO: handle arrays
    }

    private String getId (ClassType ct, String join) {
	return ct.get ().stream ().map (s -> s.getId ()).collect (Collectors.joining (join));
    }

    private String resolve (String id) {
	TreeNode type = cth.getType (id);
	if (type != null)
	    return id;

	if (isTypeParameter (id)) {
	    return id;
	}

	if (crh.hasType (id)) {
	    return id;
	}

	// check for inner class
	for (String ctn : containingTypeName) {
	    String icn = packageName == null ?
		ctn + "." + id :
		packageName.getDotName () + "." + ctn + "." + id;
	    // System.err.println ("Trying icn: " + icn);
	    type = cth.getType (icn);
	    if (type != null)
		return icn;
	}
	return resolveUsingImports (id);
    }

    private String resolveUsingImports (String id) {
	String fqn = ih.stid.get (id);
	if (fqn != null && validFullName (fqn))
	    return fqn;
	fqn = tryTypeImportOnDemand (id);
	if (fqn != null)
	    return fqn;
	/*
	fqn = trySingleStaticImport (id);
	if (fqn != null)
	    return fqn;
	fqn = tryStaticImportOnDemand (id);
	if (fqn != null)
	    return fqn;
	*/

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