package org.khelekore.parjac.tree;

import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;

public class CompilationUnit implements TreeNode {
    private final PackageDeclaration pd;
    private final List<TreeNode> imports;
    private final List<TreeNode> types;

    public CompilationUnit (Rule r, Deque<TreeNode> parts) {
	PackageDeclaration pd = null;
	List<TreeNode> imports = null;
	List<TreeNode> types = null;

	if (!parts.isEmpty ()) {
	    TreeNode tn = parts.pop ();
	    pd = tn instanceof PackageDeclaration ? (PackageDeclaration)tn : null;
	    if (pd != null)
		tn = parts.isEmpty () ? null : parts.pop ();
	    if (tn != null) {
		ZOMEntry zn = (ZOMEntry)tn;
		if (hasImports (zn)) {
		    imports = zn.get ();
		    tn = parts.isEmpty () ? null : parts.pop ();
		}
		if (tn != null)
		    types = ((ZOMEntry)tn).get ();
	    }
	}
	this.pd = pd;
	this.imports = imports;
	this.types = types;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{package: " +  pd +
	    ", imports: " + imports + ", types: " + types + "}";
    }

    private final boolean hasImports (ZOMEntry z) {
	List<TreeNode> ls = z.get ();
	return ls.get (0) instanceof ImportDeclaration;
    }

    public DottedName getPackage () {
	return pd == null ? null : pd.getPackageName ();
    }

    public List<TreeNode> getTypes () {
	return types;
    }

    public void visit (TreeVisitor visitor) {
	visitor.visit (this);
	if (types != null)
	    types.forEach (t -> t.visit (visitor));
    }
}