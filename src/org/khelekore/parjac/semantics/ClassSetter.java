package org.khelekore.parjac.semantics;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.khelekore.parjac.tree.*;

public class ClassSetter implements TreeVisitor {
    private final CompilerDiagnosticCollector diagnostics;
    private final CompiledTypesHolder cth;
    private final SyntaxTree tree;
    private final DottedName packageName;
    private final InterfaceHandler ih = new InterfaceHandler ();

    public ClassSetter (CompiledTypesHolder cth, SyntaxTree tree,
			CompilerDiagnosticCollector diagnostics) {
	this.cth = cth;
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

    @Override public void visit (ConstructorDeclaration c) {
	setTypes (c.getParameters ());
    }

    @Override public void visit (FieldDeclaration f) {
	setType (f.getType ());
    }

    @Override public void visit (MethodDeclaration m) {
	setType (m.getResult ());
	setTypes (m.getParameters ());
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
	if (type instanceof ClassType) {
	    ClassType ct = (ClassType)type;
	    String id = getId (ct);
	    String fqn = resolve (id);
	    ct.setFullName (fqn);
	}
	// TODO: handle arrays
    }

    private String getId (ClassType ct) {
	return ct.get ().stream ().map (s -> s.getId ()).collect (Collectors.joining ("."));
    }

    private String resolve (String id) {
	TreeNode type = cth.getType (id);
	if (type != null)
	    return id;

	String fqn = ih.stid.get (id);
	if (fqn != null)
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

	/*
	diagnostics.report (new NoSourceDiagnostics (tree.getOrigin () +
						     ": Failed to find class: " + id));
	*/
	return null;
    }

    private String tryTypeImportOnDemand (String id) {
	for (String p : ih.tiod) {
	    String fqn = p + "." + id;
	    if (hasType (fqn))
		return fqn;
	}
	return null;
    }

    private boolean hasType (String fqn) {
	String name = "/" + fqn.replaceAll ("\\.", "/") + ".class";
	URL u = ClassSetter.class.getResource (name);
	return u != null;
    }

    private class InterfaceHandler implements InterfaceVisitor {
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
	    tiod.add (packageName.getDotName ());
	    tiod.add ("java.lang");
	}
    }
}
