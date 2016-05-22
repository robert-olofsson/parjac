package org.khelekore.parjac.semantics;

import java.util.ArrayDeque;
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
	    owners.addLast (new FieldHolder (c));
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    owners.addLast (new FieldHolder (e));
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    owners.addLast (new FieldHolder (i));
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    owners.addLast (new FieldHolder (a));
	    return true;
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    owners.addLast (new FieldHolder (b));
	    return true;
	}

	@Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	    endType ();
	}

	@Override public void endType () {
	    FieldHolder fh = owners.removeLast ();
	    cip.registerFields (cip.getFullName (fh.owner), fh.fields);
	}

	@Override public boolean visit (FieldDeclaration f) {
	    f.getVariables ().get ().forEach (v -> owners.getLast ().add (f, v));
	    return false;
	}

	@Override public boolean visit (EnumConstant c) {
	    owners.getLast ().add (c);
	    return false;
	}
    }

    private class FieldHolder {
	private final TreeNode owner;
	private final Map<String, FieldInformation<?>> fields = new HashMap<> ();

	public FieldHolder (TreeNode owner) {
	    this.owner = owner;
	}

	public void add (FieldDeclaration f, VariableDeclarator vd) {
	    add (new FieldInformation<VariableDeclaration> (vd.getId (), f, owner));

	}

	public void add (EnumConstant c) {
	    add (new FieldInformation<EnumConstant> (c.getId (), c, owner));
	}

	private void add (FieldInformation<?> fi) {
	    FieldInformation<?> current = fields.put (fi.getName (), fi);
	    if (current != null) {
		ParsePosition fpp = current.getParsePosition ();
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), fi.getParsePosition (),
							     "Field %s already defined at %d:%d", fi.getName (),
							     fpp.getLineNumber (), fpp.getTokenColumn ()));
	    }
	}
    }
}