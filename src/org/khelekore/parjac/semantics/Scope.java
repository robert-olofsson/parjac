package org.khelekore.parjac.semantics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.FlaggedType;
import org.khelekore.parjac.tree.FormalParameter;
import org.khelekore.parjac.tree.LastFormalParameter;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.VariableDeclaration;
import org.khelekore.parjac.tree.VariableDeclarator;

public class Scope {
    private final Scope parent;
    private final boolean isStatic;
    private Map<String, FieldInformation<?>> variables = Collections.emptyMap ();

    public Scope (Scope parent, boolean isStatic) {
	this.parent = parent;
	this.isStatic = isStatic;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{parent: " + parent +
	    ", isStatic: " + isStatic + ", variables: " + variables + "}";
    }

    public boolean isStatic () {
	return isStatic;
    }

    /** Try to add a variable declaration to the current scope.
     */
    public void tryToAdd (VariableDeclaration fd, VariableDeclarator vd,
			  SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<VariableDeclaration> (vd.getId (), fd), fd, tree, diagnostics);
    }

    public void tryToAdd (FormalParameter fp, SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<FormalParameter> (fp.getId (), fp), fp, tree, diagnostics);
    }

    public void tryToAdd (LastFormalParameter fp, SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<LastFormalParameter> (fp.getId (), fp), fp, tree, diagnostics);
    }

    private void tryToAdd (FieldInformation<?> fi, FlaggedType ft,
			   SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	String name = fi.getName ();
	boolean currentScopeHas = has (name);
	FieldInformation<?> f = find (name, FlagsHelper.isStatic (ft.getFlags ()));
	if (!currentScopeHas && f != null) {
	    diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (), fi.getParsePosition (),
							   "Field %s shadows another variable", name));
	}
	if (currentScopeHas) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), fi.getParsePosition (),
							 "Field %s already defined", name));
	} else {
	    add (fi);
	}
    }

    /** Try to find a variable named id in this and all parent Scopes. */
    public FieldInformation<?> find (String id, boolean isStatic) {
	Scope s = this;
	while (s != null) {
	    isStatic |= s.isStatic;
	    FieldInformation<?> fi = s.variables.get (id);
	    if (fi != null && (!isStatic || fi.isStatic()))
		return fi;
	    s = s.parent;
	}
	return null;
    }

    public void add (FieldInformation<?> fi) {
	if (variables.isEmpty ())
	    variables = new LinkedHashMap<> ();
	variables.put (fi.getName (), fi);
    }

    /** Check if there is a variable named 'id' in this scope. */
    public boolean has (String id) {
	return variables.get (id) != null;
    }

    public Scope endScope () {
	return parent;
    }
}
