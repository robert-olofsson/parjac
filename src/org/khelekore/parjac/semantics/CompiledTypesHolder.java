package org.khelekore.parjac.semantics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
    private Map<String, NodeInformation> name2node = new HashMap<> ();
    private Map<TreeNode, NodeInformation> node2fqn = new HashMap<> ();
    private Map<TreeNode, ClassType> anonSuperClasses = new HashMap<> ();

    public void addTypes (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	tree.getCompilationUnit ().visit (new ClassMapper (tree, diagnostics));
    }

    public Set<String> getTypes () {
	return name2node.keySet ();
    }

    public LookupResult hasVisibleType (String fqn) {
	NodeInformation ni = name2node.get (fqn);
	if (ni == null)
	    return LookupResult.NOT_FOUND;
	int flags = 0;
	if (ni.tn instanceof FlaggedType)
	    flags = ((FlaggedType)ni.tn).getFlags ();
	return new LookupResult (true, flags);
    }

    /** Get the outer tree node for a given fully qualified name,
     *  that is "some.package.Foo$Bar".
     */
    public TreeNode getType (String fqn) {
	NodeInformation ni = name2node.get (fqn);
	if (ni == null)
	    return null;
	return ni.tn;
    }

    public String getFilename (TreeNode tn) {
	NodeInformation ni = node2fqn.get (tn);
	if (ni == null)
	    return null;
	return ni.filename;
    }

    /** Get the class id, something like "some.package.Foo$Bar$1". */
    public String getFullName (TreeNode tn) {
	NodeInformation ni = node2fqn.get (tn);
	if (ni == null)
	    return null;
	return ni.fqn;
    }

    public Optional<List<String>> getSuperTypes (String type) {
	TreeNode tn = getType (type);
	if (tn == null)
	    return Optional.empty ();
	if (tn instanceof NormalClassDeclaration) {
	    NormalClassDeclaration ncd = (NormalClassDeclaration)tn;
	    List<String> ret = new ArrayList<> ();
	    ClassType ct = ncd.getSuperClass ();
	    if (ct != null) {
		if (ct.getFullName () == null)
		    return Optional.of (Collections.<String>emptyList ());
		ret.add (ct.getFullName ());
	    } else {
		ret.add ("java.lang.Object");
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
	} else if (tn instanceof ClassBody) {
	    ClassType ct = anonSuperClasses.get (tn);
	    if (ct != null) {
		List<String> ret = Arrays.asList (ct.getFullName ());
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
	    pushClass (c.getId (), c);
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    pushClass (e.getId (), e);
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    pushClass (i.getId (), i);
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    pushClass (a.getId (), a);
	    return true;
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    anonSuperClasses.put (b, ct);
	    pushAnonymousClass (generateAnonId (), b);
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

	private void pushClass (String id, FlaggedType ft) {
	    pushClass (id, ft, false);
	}

	private void pushAnonymousClass (String id, TreeNode tn) {
	    pushClass (id, tn, true);
	}

	private void pushClass (String id, TreeNode tn, boolean anonymous) {
	    ClassId cid = new ClassId (id);
	    if (!anonymous && classes.contains (cid)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.getParsePosition (),
							     "Class: %s already defined", id));
	    }
	    classes.addLast (cid);
	    String fullId = getFullId ();
	    String filename = getFullFilename ();
	    String name = getFQN (packageName, fullId);
	    NodeInformation ni = new NodeInformation (tn, name, filename);
	    synchronized (name2node) {
		name2node.put (name, ni);
		node2fqn.put (tn, ni);
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

	private String getFullFilename () {
	    return classes.stream ().map (cid -> cid.id).collect (Collectors.joining ("$"));
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

    private static class NodeInformation {
	private final TreeNode tn;
	private final String fqn;
	private final String filename;

	public NodeInformation (TreeNode tn, String fqn, String filename) {
	    this.tn = tn;
	    this.fqn = fqn;
	    this.filename = filename;
	}
    }
}
