package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.CatchFormalParameter;
import org.khelekore.parjac.tree.ConstructorDeclaration;
import org.khelekore.parjac.tree.EnumConstant;
import org.khelekore.parjac.tree.FormalParameter;
import org.khelekore.parjac.tree.LambdaParameter;
import org.khelekore.parjac.tree.LastFormalParameter;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.VariableDeclaration;
import org.khelekore.parjac.tree.VariableDeclarator;

public class Scope {
    private final Scope parent;
    private final Type type;
    private final boolean isStatic;
    private final boolean ignoreFieldShadowing;
    private final TreeNode owner;
    private final TreeNode currentClass;
    private Map<String, FieldInformation<?>> variables = Collections.emptyMap ();

    public enum Type { CLASS, LOCAL };

    public Scope (TreeNode owner, Scope parent, Type type, boolean isStatic, boolean ignoreFieldShadowing) {
	this.owner = owner;
	this.parent = parent;
	this.type = type;
	this.isStatic = isStatic;
	this.ignoreFieldShadowing = ignoreFieldShadowing;
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

    public TreeNode getOwner () {
	return owner;
    }

    public boolean isStatic () {
	return isStatic;
    }

    public boolean isConstructor () {
	return owner instanceof ConstructorDeclaration;
    }

    /** Try to add a variable declaration to the current scope.
     */
    public void tryToAdd (ClassInformationProvider cip, VariableDeclaration fd, VariableDeclarator vd,
			  SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tryToAdd (cip, new FieldInformation<VariableDeclaration> (vd.getId (), fd, owner, vd.hasInitializer ()),
		  FlagsHelper.isStatic (fd.getFlags ()), tree, diagnostics);
    }

    public void tryToAdd (ClassInformationProvider cip, FormalParameter fp, SyntaxTree tree,
			  CompilerDiagnosticCollector diagnostics) {
	tryToAdd (cip, new FieldInformation<FormalParameter> (fp.getId (), fp, owner, true),
		  FlagsHelper.isStatic (fp.getFlags ()), tree, diagnostics);
    }

    public void tryToAdd (ClassInformationProvider cip, LastFormalParameter fp, SyntaxTree tree,
			  CompilerDiagnosticCollector diagnostics) {
	tryToAdd (cip, new FieldInformation<LastFormalParameter> (fp.getId (), fp, owner, true),
		  FlagsHelper.isStatic (fp.getFlags ()), tree, diagnostics);
    }

    public void tryToAdd (ClassInformationProvider cip, EnumConstant c, SyntaxTree tree,
			  CompilerDiagnosticCollector diagnostics) {
	tryToAdd (cip, new FieldInformation<EnumConstant> (c.getId (), c, owner, true),
		  FlagsHelper.isStatic (c.getFlags ()), tree, diagnostics);
    }

    public void tryToAdd (ClassInformationProvider cip, LambdaParameter lp, SyntaxTree tree,
			  CompilerDiagnosticCollector diagnostics) {
	tryToAdd (cip, new FieldInformation<LambdaParameter> (lp.getId (), lp, owner, true),
		  FlagsHelper.isStatic (lp.getFlags ()), tree, diagnostics);
    }

    public void tryToAdd (ClassInformationProvider cip, CatchFormalParameter cfp, SyntaxTree tree,
			  CompilerDiagnosticCollector diagnostics) {
	tryToAdd (cip, new FieldInformation<CatchFormalParameter> (cfp.getId (), cfp, owner, true),
		  FlagsHelper.isStatic (cfp.getFlags ()), tree, diagnostics);
    }

    private void tryToAdd (ClassInformationProvider cip, FieldInformation<?> fi, boolean isStatic,
			   SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	String name = fi.getName ();
	FindResult fr = find (cip, name, isStatic, false, null);
	if (fr != null) {
	    ParsePosition fpp = fr.fi.getParsePosition ();
	    if (!fr.passedClassScope || fr.fi.getOwner () == owner) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), fi.getParsePosition (),
							     "Field %s already defined at %d:%d", name,
							     fpp.getLineNumber (), fpp.getTokenColumn ()));
		return;
	    } else {
		if (!ignoreFieldShadowing) {
		    diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (), fi.getParsePosition (),
								   "Field %s shadows variable at %d:%d", name,
								   fpp.getLineNumber (), fpp.getTokenColumn ()));
		}
	    }
	}
	add (fi);
    }

    /** Try to find a variable named id in this and all parent Scopes. */
    public FindResult find (ClassInformationProvider cip, String id,
			    boolean isStatic, boolean checkSuperClasses,
			    CompilerDiagnosticCollector diagnostics) {
	boolean passedClassScope = false;
	Scope s = this;
	while (s != null) {
	    FieldInformation<?> fi;
	    if (s.type == Type.CLASS) {
		passedClassScope = true;
		fi = cip.getFieldInformation (cip.getFullName (s.owner), id);
		if (fi == null && checkSuperClasses) {
		    try {
			fi = checkSuperClasses (cip, s.owner, id);
		    } catch (IOException e) {
			diagnostics.report (new NoSourceDiagnostics ("Failed to load class: " + s.owner, e));
		    }
		}
	    } else {
		fi = s.variables.get (id);
	    }
	    if (fi != null && (s.type == Type.LOCAL || !isStatic || fi.isStatic ()))
		return new FindResult (fi, s.currentClass, passedClassScope, s);
	    isStatic |= s.isStatic;
	    s = s.parent;
	}
	return null;
    }

    private FieldInformation<?> checkSuperClasses (ClassInformationProvider cip, TreeNode owner, String id)
	throws IOException {
	Deque<String> superClasses = new ArrayDeque<> ();
	addSupers (cip, superClasses, cip.getFullName (owner));
	while (!superClasses.isEmpty ()) {
	    String sclz = superClasses.removeFirst ();
	    FieldInformation<?> fi = cip.getFieldInformation (sclz, id);
	    if (fi != null)
		return fi;
	    addSupers (cip, superClasses, sclz);
	}
	return null;
    }

    private void addSupers (ClassInformationProvider cip, Deque<String> superClasses, String clz) throws IOException {
	Optional<List<String>> supers = cip.getSuperTypes (clz, false);
	if (supers.isPresent ()) {
	    superClasses.addAll (supers.get ());
	}
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
	return variables;
    }

    public boolean isInitialized (FieldInformation<?> fi) {
	return fi.isInitialized ();
    }

    /* qwerty
    public void setAssigned (FieldInformation<?> fi) {
	System.err.println ("setting assigned: "+ fi+ ", optionallyAssigned: " + optionallyAssigned);
	if (optionallyAssigned != null) {
	    optionallyAssigned.getLast ().add (fi);
	} else {
	    definatelyAssigned.add (fi);
	}
    }


    public void optionalAssignment () {
	System.err.println ("Starting optional assignment");
	if (optionallyAssigned == null)
	    optionallyAssigned = new ArrayDeque<> ();
	optionallyAssigned.add (new HashSet<> ());
    }

    public void makeDefinate (boolean keepDefined) {
	System.err.println ("Making definate: " + keepDefined);
	if (keepDefined) {
	    Set<FieldInformation<?>> o = optionallyAssigned.removeFirst ();
	    for (Set<FieldInformation<?>> b : optionallyAssigned)
		o.retainAll (b);
	    definatelyAssigned.addAll (o);
	}
	optionallyAssigned = null;
    }
    */

    public static class FindResult {
	public final FieldInformation<?> fi;
	public final TreeNode containingClass;
	public final boolean passedClassScope;
	public final Scope scope;

	public FindResult (FieldInformation<?> fi, TreeNode containingClass, boolean passedClassScope, Scope scope) {
	    this.fi = fi;
	    this.containingClass = containingClass;
	    this.passedClassScope = passedClassScope;
	    this.scope = scope;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{fi: " + fi +
		", passedClassScope: " + passedClassScope +
		", containingClass: " + containingClass.getClass ().getName () + "}";
	}
    }
}
