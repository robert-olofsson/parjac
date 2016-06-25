package org.khelekore.parjac.semantics;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.EnumConstant;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SiblingVisitor;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;
import org.khelekore.parjac.tree.VariableDeclaration;
import org.khelekore.parjac.tree.VariableDeclarator;

public class FieldRegistrator {
    private final ClassInformationProvider cip;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;

    public FieldRegistrator (ClassInformationProvider cip, SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void findFields () {
	MemberVisitor mv = new MemberVisitor ();
	tree.getCompilationUnit ().visit (mv);
    }

    private class MemberVisitor implements TreeVisitor {
	private final Deque<FieldHolder> owners = new ArrayDeque<> ();

	@Override public boolean visit (NormalClassDeclaration c) {
	    addFields (c, c.getBody ());
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    addFields (e, e.getBody ());
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    addFields (i, i.getBody ());
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    addFields (a, a.getBody ());
	    return true;
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    addFields (b, b);
	    return true;
	}

	@Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	    endType ();
	}

	private void addFields (TreeNode type, TreeNode body) {
	    owners.addLast (new FieldHolder (type));
	    body.visit (new FieldCollector (owners));
	}

	@Override public void endType () {
	    FieldHolder fh = owners.removeLast ();
	    cip.registerFields (cip.getFullName (fh.owner), fh.fields);
	}
    }

    private class FieldCollector extends SiblingVisitor {
	private final Deque<FieldHolder> owners;

	public FieldCollector (Deque<FieldHolder> owners) {
	    this.owners = owners;
	}

	@Override public boolean visit (FieldDeclaration f) {
	    f.getVariables ().get ().forEach (v -> owners.getLast ().add (owners, f, v));
	    return false;
	}

	@Override public boolean visit (EnumConstant c) {
	    owners.getLast ().add (owners, c);
	    return false;
	}
    }

    private static final Map<String, FieldInformation<?>> EMPTY = Collections.unmodifiableMap (Collections.emptyMap ());

    private class FieldHolder {
	private final TreeNode owner;
	private Map<String, FieldInformation<?>> fields = EMPTY;

	public FieldHolder (TreeNode owner) {
	    this.owner = owner;
	}

	public void add (Deque<FieldHolder> owners, FieldDeclaration f, VariableDeclarator vd) {
	    add (owners, new FieldInformation<VariableDeclaration> (vd.getId (), f, owner, vd.hasInitializer ()));
	}

	public void add (Deque<FieldHolder> owners, EnumConstant c) {
	    add (owners, new FieldInformation<EnumConstant> (c.getId (), c, owner, true));
	}

	private void add (Deque<FieldHolder> owners, FieldInformation<?> fi) {
	    if (fields.isEmpty ())
		fields = new HashMap<> ();
	    FieldInformation<?> current = fields.put (fi.getName (), fi);
	    if (current != null) {
		ParsePosition fpp = current.getParsePosition ();
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), fi.getParsePosition (),
							     "Field %s already defined at %d:%d", fi.getName (),
							     fpp.getLineNumber (), fpp.getTokenColumn ()));
	    }

	    for (FieldHolder fh : owners) {
		if (fh == this)
		    continue;
		FieldInformation<?> other = fh.fields.get (fi.getName ());
		if (other != null) {
		    ParsePosition fpp = other.getParsePosition ();
		    diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (), fi.getParsePosition (),
								   "Field %s shadows variable at %d:%d", fi.getName (),
								   fpp.getLineNumber (), fpp.getTokenColumn ()));
		}
	    }
	}
    }
}