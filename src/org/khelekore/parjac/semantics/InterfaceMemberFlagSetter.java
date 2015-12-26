package org.khelekore.parjac.semantics;

import java.util.ArrayDeque;
import java.util.Deque;

import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.FlaggedTypeBase;
import org.khelekore.parjac.tree.MethodDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;

public class InterfaceMemberFlagSetter {
    private final SyntaxTree tree;

    public InterfaceMemberFlagSetter (SyntaxTree tree) {
	this.tree = tree;
    }

    public void reflag () {
	MemberVisitor mv = new MemberVisitor ();
	tree.getCompilationUnit ().visit (mv);
    }

    private class MemberVisitor implements TreeVisitor {
	private Deque<Boolean> types = new ArrayDeque<> ();

	public MemberVisitor () {
	    types.push (Boolean.FALSE);
	}

	@Override public boolean visit (NormalClassDeclaration c) {
	    reflag (c);
	    types.push (Boolean.FALSE);
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    reflag (e);
	    types.push (Boolean.FALSE);
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    reflag (i);
	    types.push (Boolean.TRUE);
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    reflag (a);
	    types.push (Boolean.FALSE);
	    return true;
	}

	@Override public void endType () {
	    types.pop ();
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    types.push (Boolean.FALSE);
	    return true;
	}

	@Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	    types.pop ();
	}

	@Override public void visit (FieldDeclaration f) {
	    reflag (f);
	}

	@Override public boolean visit (MethodDeclaration m) {
	    reflag (m);
	    return true;
	}

	private void reflag (FlaggedTypeBase ft) {
	    if (types.peek ()) {
		int flags = ft.getFlags ();
		if (!FlagsHelper.isPrivate (flags) && !FlagsHelper.isProtected (flags))
		    ft.makePublic ();
	    }
	}
    }
}