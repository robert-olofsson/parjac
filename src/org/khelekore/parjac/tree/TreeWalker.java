package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac.lexer.ParsePosition;

public class TreeWalker {

    public static void walkTopDown (TreeVisitor visitor, TreeNode root) {
	List<TreeNode> nodes = new ArrayList<> ();
	preOrder (root, nodes);
	nodes.forEach (tn -> tn.simpleVisit (visitor));
    }

    private static void preOrder (TreeNode tn, List<TreeNode> nodes) {
	nodes.add (tn);
	tn.getChildNodes ().forEach (cn -> preOrder (cn, nodes));
    }

    public static void walkBottomUp (TreeVisitor visitor, TreeNode root) {
	List<TreeNode> nodes = new ArrayList<> ();
	Typer t = new Typer ();
	postOrder (root, nodes, t);
	nodes.forEach (tn -> tn.simpleVisit (visitor));
    }

    private static void postOrder (TreeNode tn, List<TreeNode> nodes, Typer t) {
	t.clear ();
	tn.simpleVisit (t);
	if (t.currentClass != null)
	    nodes.add (t.currentClass);
	TreeNode ec = t.endClass;
	tn.getChildNodes ().forEach (cn -> postOrder (cn, nodes, t));
	if (ec == null)
	    nodes.add (tn);
	else
	    nodes.add (ec);
    }

    private static class Typer implements TreeVisitor {
	private TreeNode currentClass;
	private TreeNode endClass;

	@Override public boolean visit (NormalClassDeclaration c) {
	    classDef (c);
	    return false;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    classDef (e);
	    return false;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    classDef (i);
	    return false;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    classDef (a);
	    return false;
	}

	private void classDef (TreeNode t) {
	    currentClass = new Starter (t);
	    endClass = new Ender (t);
	}

	@Override public void visit (ClassInstanceCreationExpression c) {
	}

	public void clear () {
	    currentClass = null;
	    endClass = null;
	}
    }

    private static class FakeNode implements TreeNode {
	protected final TreeNode t;

	public FakeNode (TreeNode t) {
	    this.t = t;
	}

	@Override public ParsePosition getParsePosition () {
	    return t.getParsePosition ();
	}
    }

    private static class Starter extends FakeNode {
	public Starter (TreeNode t) {
	    super (t);
	}

	@Override public void simpleVisit (TreeVisitor visitor) {
	    t.simpleVisit (visitor);
	}
    }

    private static class Ender extends FakeNode {
	public Ender (TreeNode t) {
	    super (t);
	}

	@Override public void simpleVisit (TreeVisitor visitor) {
	    visitor.endType ();
	}
    }
}