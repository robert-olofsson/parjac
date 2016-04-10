package org.khelekore.parjac.semantics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.ArgumentList;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.ConstructorArguments;
import org.khelekore.parjac.tree.ConstructorBody;
import org.khelekore.parjac.tree.ConstructorDeclaration;
import org.khelekore.parjac.tree.ConstructorDeclarator;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExplicitConstructorInvocation;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;

public class AddImplicitMethods {
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;

    public AddImplicitMethods (SyntaxTree tree,
			       CompilerDiagnosticCollector diagnostics) {
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void run () {
	Finder f = new Finder ();
	tree.getCompilationUnit ().visit (f);
    }

    private class Finder implements TreeVisitor {
	private Deque<ConstructorCollector<?>> ccs = new ArrayDeque<> ();

	@Override public boolean visit (NormalClassDeclaration c) {
	    ccs.addLast (new ClassConstructorCollector (c));
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    ccs.addLast (new EnumConstructorCollector (e));
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    ccs.addLast (new NoAddCollector (i));
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    ccs.addLast (new NoAddCollector (a));
	    return true;
	}

	@Override public void endType () {
	    ConstructorCollector<?> cc = ccs.removeLast ();
	    if (cc == null)
		return;
	    if (cc.isEmpty ())
		cc.addDefaultConstructor ();
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    ccs.addLast (new NoAddCollector (b));
	    return true;
	}

	@Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	    ccs.removeLast ();
	}

	@Override public boolean visit (ConstructorDeclaration c) {
	    ccs.getLast ().add (c);
	    return true;
	}
    }

    private abstract class ConstructorCollector<T> {
	private final List<ConstructorDeclaration> cds = new ArrayList<> ();

	public void add (ConstructorDeclaration cd) {
	    cds.add (cd);
	    String cdid = cd.getId ();
	    if (!getId ().equals (cdid))
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), cd.getParsePosition (),
							     "Constructor name not matching class name"));
	}

	public boolean isEmpty () {
	    return cds.isEmpty ();
	}

	/** Get the id/class name of the type */
	abstract String getId ();

	abstract void addDefaultConstructor ();
    }

    private class ClassConstructorCollector extends ConstructorCollector<NormalClassDeclaration> {
	private final NormalClassDeclaration type;

	public ClassConstructorCollector (NormalClassDeclaration type) {
	    this.type = type;
	}

	public String getId () {
	    return type.getId ();
	}

	public void addDefaultConstructor () {
	    ConstructorDeclarator d = new ConstructorDeclarator (null, getId (), null);
	    ArgumentList al = new ArgumentList (Collections.emptyList ());
	    ConstructorArguments ca = new ConstructorArguments (al);
	    ExplicitConstructorInvocation eci = new ExplicitConstructorInvocation (null, Token.SUPER, ca);
	    ConstructorBody body = new ConstructorBody (eci, null);
	    ConstructorDeclaration cd = new ConstructorDeclaration (FlagsHelper.isPublic (type.getFlags ()), d, null, body);

	    ClassBody cb = type.getBody ();
	    cb.getDeclarations ().add (cd);
	}
    }

    private class EnumConstructorCollector extends ConstructorCollector<EnumDeclaration> {
	private final EnumDeclaration type;
	public EnumConstructorCollector (EnumDeclaration type) {
	    this.type = type;
	}

	public String getId () {
	    return type.getId ();
	}

	public void addDefaultConstructor () {
	    // TODO: implement
	}
    }

    private class NoAddCollector extends ConstructorCollector<Void> {
	private final TreeNode type;

	public NoAddCollector (TreeNode type) {
	    this.type = type;
	}

	public void add (ConstructorDeclaration cd) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), type.getParsePosition (),
							 "May not add constructor to: " +
							 type.getClass ().getSimpleName ()));

	}

	public String getId () {
	    return null;
	}

	public void addDefaultConstructor () {
	    // TODO: implement
	}
    }
}
