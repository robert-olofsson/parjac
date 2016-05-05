package org.khelekore.parjac.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class PrimaryNoNewArray {
    public static class ThisPrimary extends PositionNode {
	private ExpressionType expType;

	public ThisPrimary (ParsePosition pos) {
	    super (pos);
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{}";
	}

	public void setExpressionType (ExpressionType expType) {
	    this.expType = expType;
	}

	@Override public ExpressionType getExpressionType () {
	    return expType;
	}

	@Override public void visit (TreeVisitor visitor) {
	    visitor.visit (this);
	}
    }

    public static class DottedThis extends PositionNode {
	private final TreeNode name;

	public DottedThis (TreeNode name, ParsePosition pos) {
	    super (pos);
	    this.name = name;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + name + ".this}";
	}

	@Override public void visit (TreeVisitor visitor) {
	    visitor.visit (this);
	    name.visit (visitor);
	}

	@Override public void simpleVisit (TreeVisitor visitor) {
	    visitor.visit (this);
	}

	@Override public ExpressionType getExpressionType () {
	    return name.getExpressionType ();
	}

	@Override public Collection<? extends TreeNode> getChildNodes () {
	    return Collections.emptyList ();
	}
    }

    public static class VoidClass extends PositionNode {
	public VoidClass (ParsePosition pos) {
	    super (pos);
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{}";
	}

	@Override public void visit (TreeVisitor visitor) {
	    visitor.visit (this);
	}

	@Override public void simpleVisit (TreeVisitor visitor) {
	    visitor.visit (this);
	}

	@Override public ExpressionType getExpressionType () {
	    return ExpressionType.CLASS;
	}

	@Override public Collection<? extends TreeNode> getChildNodes () {
	    return Collections.emptyList ();
	}
    }

    public static class ClassPrimary extends PositionNode {
	private final TreeNode type;
	private final List<Brackets> brackets;

	public ClassPrimary (TreeNode type, List<Brackets> brackets, ParsePosition pos) {
	    super (pos);
	    this.type = type;
	    this.brackets = brackets;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + type + "  " + brackets + ".class}";
	}

	@Override public void visit (TreeVisitor visitor) {
	    if (visitor.visit (this))
		type.visit (visitor);
	}

	@Override public void simpleVisit (TreeVisitor visitor) {
	    visitor.visit (this);
	}

	@Override public ExpressionType getExpressionType () {
	    return ExpressionType.CLASS;
	}

	@Override public Collection<? extends TreeNode> getChildNodes () {
	    return Collections.singleton (type);
	}

	public TreeNode getType () {
	    return type;
	}
    }

    public static TreeNode build (Rule r, Deque<TreeNode> parts, ParsePosition pos) {
	if (r.getRulePart (0).getId () == Token.THIS)
	    return new ThisPrimary (pos);
	if (r.size () == 1)
	    return parts.pop ();
	if (r.getRulePart (0).getId () == Token.LEFT_PARENTHESIS)
	    return parts.pop ();
	if (r.getRulePart (2).getId () == Token.THIS)
	    return new DottedThis (parts.pop (), pos);
	if (r.getRulePart (0).getId () == Token.VOID)
	    return new VoidClass (pos);
	return new ClassPrimary (parts.pop (), r.size () > 3 ? ((ZOMEntry)parts.pop ()).get () : null, pos);
    }
}
