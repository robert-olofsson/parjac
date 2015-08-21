package org.khelekore.parjac.semantics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.tree.*;

public class CompiledTypesHolder {
    private Map<String, TreeNode> name2node = new HashMap<> ();
    private Map<TreeNode, String> node2fqn = new HashMap<> ();

    public void addTypes (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tree.getCompilationUnit ().visit (new ClassMapper (tree, diagnostics));
    }

    public Set<String> getTypes () {
	return name2node.keySet ();
    }

    public boolean hasType (String fqn) {
	return name2node.containsKey (fqn);
    }

    /** Get the outer tree node for a given fully qualified name,
     *  that is "some.package.Foo$Bar".
     */
    public TreeNode getType (String fqn) {
	return name2node.get (fqn);
    }

    public String getFilename (TreeNode tn) {
	return getFullName (tn);
    }

    /** Get the class id, something like "some.package.Foo$Bar$1". */
    public String getFullName (TreeNode tn) {
	return node2fqn.get (tn);
    }

    public Optional<List<String>> getSuperTypes (String type) {
	TreeNode tn = getType (type);
	if (tn instanceof NormalClassDeclaration) {
	    NormalClassDeclaration ncd = (NormalClassDeclaration)tn;
	    List<String> ret = new ArrayList<> ();
	    ClassType ct = ncd.getSuperClass ();
	    if (ct != null) {
		if (ct.getFullName () == null)
		    return Optional.of (Collections.<String>emptyList ());
		ret.add (ct.getFullName ());
	    }
	    InterfaceTypeList ifs = ncd.getSuperInterfaces ();
	    if (ifs != null)
		ifs.get ().forEach (ic -> ret.add (ic.getFullName ()));
	    return Optional.of (ret);
	} else if (tn instanceof NormalInterfaceDeclaration) {
	    NormalInterfaceDeclaration nid = (NormalInterfaceDeclaration)tn;
	    ExtendsInterfaces ei = nid.getExtendsInterfaces ();
	    if (ei != null) {
		List<ClassType> cts = ei.get ().get ();
		List<String> ret =
		    cts.stream ().map (ct -> ct.getFullName ()).collect (Collectors.toList ());
		return Optional.of (ret);
	    }
	}
	return Optional.empty ();
    }

    private class ClassMapper implements TreeVisitor {
	private final SyntaxTree tree;
	private final CompilerDiagnosticCollector diagnostics;
	private DottedName packageName;
	private final Deque<ClassId> classes = new ArrayDeque<> ();

	public ClassMapper (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	    this.tree = tree;
	    this.diagnostics = diagnostics;
	}

	@Override public boolean visit (CompilationUnit cu) {
	    packageName = cu.getPackage ();
	    return true;
	}

	@Override public boolean visit (NormalClassDeclaration c) {
	    pushClass (c.getId (), c, false);
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    pushClass (e.getId (), e, false);
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    pushClass (i.getId (), i, false);
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    pushClass (a.getId (), a, false);
	    return true;
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    pushClass (generateAnonId (), b, true);
	    return true;
	}

	@Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	    endType ();
	}

	private String generateAnonId () {
	    ClassId cid = classes.peekLast ();
	    cid.anonId++;
	    return Integer.toString (cid.anonId);
	}

	@Override public void endType () {
	    classes.removeLast ();
	}

	private void pushClass (String id, TreeNode tn, boolean anonymous) {
	    ClassId cid = new ClassId (id);
	    if (!anonymous && classes.contains (cid)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.getParsePosition (),
							     "Class: %s already defined", id));
	    }
	    classes.addLast (cid);
	    String fullId = getFullId ();
	    String name = getFQN (packageName, fullId);
	    synchronized (name2node) {
		name2node.put (name, tn);
		node2fqn.put (tn, name);
	    }
	}

	private String getFullId () {
	    return classes.stream ().map (cid -> cid.id).collect (Collectors.joining ("$"));
	}

	public String getFQN (DottedName packageName, String fullId) {
	    if (packageName == null)
		return fullId;
	    return packageName.getDotName () + "." + fullId;
	}
    }

    private static class ClassId {
	private final String id;
	private int anonId;

	public ClassId (String id) {
	    this.id = id;
	}

	@Override public int hashCode () {
	    return id.hashCode ();
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () != getClass ())
		return false;
	    ClassId c = (ClassId)o;
	    return Objects.equals (id, c.id);
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{id: " + id + ", anonId: " + anonId + "}";
	}
    }
}
