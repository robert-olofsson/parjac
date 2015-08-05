package org.khelekore.parjac.semantics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.BasicForStatement;
import org.khelekore.parjac.tree.Block;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.ConstructorDeclaration;
import org.khelekore.parjac.tree.EnhancedForStatement;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.LocalVariableDeclaration;
import org.khelekore.parjac.tree.MethodDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;
import org.khelekore.parjac.tree.VariableDeclaration;
import org.khelekore.parjac.tree.VariableDeclarator;

/** Store fields and methods and generate errors for duplicates.
 */
public class FieldAndMethodScanner implements TreeVisitor {
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;
    private Scope currentScope = null;

    public FieldAndMethodScanner (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void scan () {
	tree.getCompilationUnit ().visit (this);
    }

    @Override public boolean visit (NormalClassDeclaration c) {
	currentScope = new Scope (currentScope);
	return true;
    }

    @Override public boolean visit (EnumDeclaration e) {
	currentScope = new Scope (currentScope);
	return true;
    }

    @Override public boolean visit (NormalInterfaceDeclaration i) {
	currentScope = new Scope (currentScope);
	return true;
    }

    @Override public boolean visit (AnnotationTypeDeclaration a) {
	currentScope = new Scope (currentScope);
	return true;
    }

    @Override public void endType () {
	currentScope = currentScope.endScope ();
    }

    @Override public boolean anonymousClass (ClassType ct, ClassBody b) {
	currentScope = new Scope (currentScope);
	return true;
    }

    @Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	currentScope = currentScope.endScope ();
    }

    @Override public void visit (FieldDeclaration f) {
	f.getVariables ().get ().forEach (v -> storeField (f, v));
    }

    @Override public boolean visit (ConstructorDeclaration c) {
	currentScope = new Scope (currentScope);
	return true;
    }

    @Override public void endConstructor (ConstructorDeclaration c) {
	currentScope = currentScope.endScope ();
    }

    @Override public boolean visit (MethodDeclaration m) {
	currentScope = new Scope (currentScope);
	// TODO: store method information
	return true;
    }

    @Override public void endMethod (MethodDeclaration m) {
	currentScope = currentScope.endScope ();
    }

    @Override public void visit (LocalVariableDeclaration l) {
	l.getVariables ().get ().forEach (v -> storeField (l, v));
    }

    @Override public boolean visit (Block b) {
	currentScope = new Scope (currentScope);
	return true;
    }

    @Override public void endBlock () {
	currentScope = currentScope.endScope ();
    }

    @Override public boolean visit (BasicForStatement f) {
	currentScope = new Scope (currentScope);
	TreeNode init = f.getForInit ();
	if (init != null)
	    init.visit (this);
	visitStatementsWithoutScope (f.getStatement ());
	currentScope = currentScope.endScope ();
	return false;
    }

    @Override public boolean visit (EnhancedForStatement f) {
	currentScope = new Scope (currentScope);
	visit (f.getLocalVariableDeclaration ());
	visitStatementsWithoutScope (f.getStatement ());
	currentScope = currentScope.endScope ();
	return false;
    }

    private void visitStatementsWithoutScope (TreeNode tn) {
	if (tn instanceof Block) {
	    Block b = (Block)tn;
	    List<TreeNode> statements = b.getStatements ();
	    if (statements != null)
		statements.forEach (s -> s.visit (this));
	} else {
	    tn.visit (this);
	}
    }

    private void storeField (VariableDeclaration fd, VariableDeclarator vd) {
	FieldInformation fi = new FieldInformation (vd.getId (), fd);
	boolean currentScopeHas = currentScope.has (fi.name);
	FieldInformation f = currentScope.find (fi.name);
	if (!currentScopeHas && f != null) {
	    diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (), vd.getParsePosition (),
							   "Field %s shadows another variable", fi.name));
	}
	if (currentScopeHas) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), vd.getParsePosition (),
							 "Field %s already defined", fi.name));
	} else {
	    currentScope.add (fi);
	}
    }

    private static class Scope {
	private final Scope parent;
	private Map<String, FieldInformation> variables = Collections.emptyMap ();

	public Scope (Scope parent) {
	    this.parent = parent;
	}

	public FieldInformation find (String id) {
	    Scope s = this;
	    while (s != null) {
		FieldInformation fi = s.variables.get (id);
		if (fi != null)
		    return fi;
		s = s.parent;
	    }
	    return null;
	}

	public void add (FieldInformation fi) {
	    if (variables.isEmpty ())
		variables = new LinkedHashMap<> ();
	    variables.put (fi.name, fi);
	}

	public boolean has (String id) {
	    return variables.get (id) != null;
	}

	public Scope endScope () {
	    return parent;
	}
    }

    private static class FieldInformation {
	private final String name;

	public FieldInformation (String name, VariableDeclaration field) {
	    this.name = name;
	    // TODO: probably want to store field here
	}
    }
}
