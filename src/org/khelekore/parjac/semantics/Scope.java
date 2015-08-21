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
    private final Type type;
    private final boolean isStatic;
    private final int classLevel;
    private Map<String, FieldInformation<?>> variables = Collections.emptyMap ();

    public enum Type { CLASS, LOCAL };

    public Scope (Scope parent, Type type, boolean isStatic) {
	this.parent = parent;
	this.type = type;
	this.isStatic = isStatic;
	if (parent == null) {
	    classLevel = 0;
	} else {
	    int add = 0;
	    if (type == Type.CLASS)
		add++;
	    if (parent.type == Type.CLASS)
		add++;
	    classLevel = parent.classLevel + add;
	}
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{parent: " + parent +
	    ", type: "  + type + ", classLevel: " + classLevel + ", isStatic: " + isStatic +
	    ", variables: " + variables + "}";
    }

    public boolean isStatic () {
	return isStatic;
    }

    /** Try to add a variable declaration to the current scope.
     */
    public void tryToAdd (VariableDeclaration fd, VariableDeclarator vd,
			  SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<VariableDeclaration> (vd.getId (), fd, classLevel),
		  fd, tree, diagnostics);
    }

    public void tryToAdd (FormalParameter fp, SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<FormalParameter> (fp.getId (), fp, classLevel),
		  fp, tree, diagnostics);
    }

    public void tryToAdd (LastFormalParameter fp, SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<LastFormalParameter> (fp.getId (), fp, classLevel),
		  fp, tree, diagnostics);
    }

    private void tryToAdd (FieldInformation<?> fi, FlaggedType ft,
			   SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	String name = fi.getName ();
	FieldInformation<?> f = find (name, FlagsHelper.isStatic (ft.getFlags ()));
	if (f != null) {
	    if (f.getClassLevel () == classLevel) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), fi.getParsePosition (),
							     "Field %s already defined", name));
		return;
	    } else {
		diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (), fi.getParsePosition (),
							       "Field %s shadows another variable", name));
	    }
	}
	add (fi);
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
