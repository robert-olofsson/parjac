package org.khelekore.parjac.semantics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.EnumConstant;
import org.khelekore.parjac.tree.FormalParameter;
import org.khelekore.parjac.tree.LastFormalParameter;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.VariableDeclaration;
import org.khelekore.parjac.tree.VariableDeclarator;

public class Scope {
    private final Scope parent;
    private final Type type;
    private final boolean isStatic;
    private final TreeNode owner;
    private final TreeNode currentClass;
    private Map<String, FieldInformation<?>> variables = Collections.emptyMap ();

    public enum Type { CLASS, LOCAL };

    public Scope (TreeNode owner, Scope parent, Type type, boolean isStatic) {
	this.owner = owner;
	this.parent = parent;
	this.type = type;
	this.isStatic = isStatic;
	if (type == Type.CLASS)
	    currentClass = owner;
	else
	    currentClass = parent.currentClass;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{owner: " + owner +
	    ", parent: " + parent + ", type: "  + type + ", isStatic: " + isStatic +
	    ", currentClass: " + currentClass + ", variables: " + variables + "}";
    }

    public boolean isStatic () {
	return isStatic;
    }

    /** Try to add a variable declaration to the current scope.
     */
    public void tryToAdd (VariableDeclaration fd, VariableDeclarator vd,
			  SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<VariableDeclaration> (vd.getId (), fd, owner),
		  FlagsHelper.isStatic (fd.getFlags ()), tree, diagnostics);
    }

    public void tryToAdd (FormalParameter fp, SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<FormalParameter> (fp.getId (), fp, owner),
		  FlagsHelper.isStatic (fp.getFlags ()), tree, diagnostics);
    }

    public void tryToAdd (LastFormalParameter fp, SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<LastFormalParameter> (fp.getId (), fp, owner),
		  FlagsHelper.isStatic (fp.getFlags ()), tree, diagnostics);
    }

    public void tryToAdd (EnumConstant c, SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (new FieldInformation<EnumConstant> (c.getId (), c, owner),
		  FlagsHelper.isStatic (c.getFlags ()), tree, diagnostics);
    }

    private void tryToAdd (FieldInformation<?> fi, boolean isStatic,
			   SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	String name = fi.getName ();
	FindResult fr = find (name, isStatic);
	if (fr != null) {
	    ParsePosition fpp = fr.fi.getParsePosition ();
	    if (!fr.passedClassScope || fr.fi.getOwner () == owner) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), fi.getParsePosition (),
							     "Field %s already defined at %d:%d", name,
							     fpp.getLineNumber (), fpp.getTokenColumn ()));
		return;
	    } else {
		diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (), fi.getParsePosition (),
							       "Field %s shadows variable at %d:%d", name,
							       fpp.getLineNumber (), fpp.getTokenColumn ()));
	    }
	}
	add (fi);
    }

    /** Try to find a variable named id in this and all parent Scopes. */
    public FindResult find (String id, boolean isStatic) {
	boolean passedClassScope = false;
	Scope s = this;
	while (s != null) {
	    passedClassScope |= s.type == Type.CLASS;
	    FieldInformation<?> fi = s.variables.get (id);
	    if (fi != null && (s.type == Type.LOCAL || !isStatic || fi.isStatic ()))
		return new FindResult (fi, s.currentClass, passedClassScope);
	    isStatic |= s.isStatic;
	    s = s.parent;
	}
	return null;
    }

    private void add (FieldInformation<?> fi) {
	if (variables.isEmpty ())
	    variables = new LinkedHashMap<> ();
	variables.put (fi.getName (), fi);
    }

    public Scope endScope () {
	return parent;
    }

    public Map<String, FieldInformation<?>> getVariables () {
	return Collections.unmodifiableMap (variables);
    }

    public static class FindResult {
	public final FieldInformation<?> fi;
	public final TreeNode containingClass;
	public final boolean passedClassScope;

	public FindResult (FieldInformation<?> fi, TreeNode containingClass, boolean passedClassScope) {
	    this.fi = fi;
	    this.passedClassScope = passedClassScope;
	    this.containingClass = containingClass;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{fi: " + fi +
		", passedClassScope: " + passedClassScope +
		", containingClass: " + containingClass.getClass ().getName () + "}";
	}
    }
}
