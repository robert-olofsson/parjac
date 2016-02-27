package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.tree.AdditionalBound;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TypeBound;
import org.khelekore.parjac.tree.TypeParameter;

public class ClassInformationProvider {
    private final ClassResourceHolder crh;
    private final CompiledTypesHolder cth;
    private Map<String, Map<String, FieldInformation<?>>> classFields;
    private Map<String, Map<String, List<MethodInformation>>> classMethods;
    private Map<TypeParameter, String> typeToName;
    private Map<String, TypeParameter> nameToType;

    public ClassInformationProvider (ClassResourceHolder crh, CompiledTypesHolder cth) {
	this.crh = crh;
	this.cth = cth;
	classFields = new ConcurrentHashMap<> ();
	classMethods = new ConcurrentHashMap<> ();
	typeToName = new ConcurrentHashMap<> ();
	nameToType = new ConcurrentHashMap<> ();
    }

    public LookupResult hasVisibleType (String fqn) {
	LookupResult res = cth.hasVisibleType (fqn);
	if (res.getFound ())
	    return res;
	return crh.hasVisibleType (fqn);
    }

    public void scanClassPath () throws IOException {
	crh.scanClassPath ();
    }

    public void addTypes (SyntaxTree tree, CompilerDiagnosticCollector diagnostics) {
	cth.addTypes (tree, diagnostics);
    }

    public TreeNode getType (String fqn) {
	return cth.getType (fqn);
    }

    public String getFullName (TreeNode tn) {
	return cth.getFullName (tn);
    }

    public String getFilename (TreeNode tn) {
	return cth.getFilename (tn);
    }

    public Optional<List<String>> getSuperTypes (String fqn) throws IOException {
	Optional<List<String>> supers = cth.getSuperTypes (fqn);
	if (supers.isPresent ())
	    return supers;
	TypeParameter tp = nameToType.get (fqn);
	if (tp != null)
	    return Optional.of (getSuperTypes (tp));
	return crh.getSuperTypes (fqn);
    }

    private List<String> getSuperTypes (TypeParameter tp) {
	TypeBound b = tp.getTypeBound ();
	if (b == null)
	    return Collections.singletonList ("java.lang.Object");
	List<String> ret = new ArrayList<> (b.size ());
	ret.add (b.getType ().getFullName ());
	List<AdditionalBound> ls = b.getAdditionalBounds ();
	if (ls != null) {
	    for (AdditionalBound ab : ls)
		ret.add (ab.getType ().getFullName ());
	}
	return ret;
    }

    public int getFlags (String fqn) {
	// TODO: need to make sure I can merge cth and crh and get some interface type back
	// TODO: with appropriate methods on it.
	TreeNode tn;
	if ((tn = getType (fqn)) != null) {
	    // TODO: this can probably fail
	    NormalClassDeclaration clz = (NormalClassDeclaration)tn;
	    return clz.getFlags ();
	} else {
	    return crh.getClassModifiers (fqn);
	}
    }

    public void registerFields (String fqn, Map<String, FieldInformation<?>> fields) {
	classFields.put (fqn, fields);
    }

    public void registerMethods (String fqn, Map<String, List<MethodInformation>> methods) {
	classMethods.put (fqn, methods);
    }

    public FieldInformation<?> getFieldInformation (String fqn, String field) {
	if ((getType (fqn)) != null) {
	    Map<String, FieldInformation<?>> m = classFields.get (fqn);
	    if (m == null)
		return null;
	    return m.get (field);
	} else {
	    throw new IllegalStateException ("not implemented yet");
	}
    }

    public ExpressionType getFieldType (String fqn, String field) {
	if ((getType (fqn)) != null) {
	    Map<String, FieldInformation<?>> m = classFields.get (fqn);
	    if (m == null)
		return null;
	    FieldInformation<?> fi = m.get (field);
	    if (fi == null)
		return null;
	    return fi.getExpressionType ();
	} else {
	    return crh.getFieldType (fqn, field);
	}
    }

    public Map<String, List<MethodInformation>> getMethods (String fqn) {
	TreeNode tn = getType (fqn);
	if (tn != null) {
	    return classMethods.get (fqn);
	} else {
	    TypeParameter tp = nameToType.get (fqn);
	    if (tp != null) {
		// No direct methods
		return Collections.emptyMap ();
	    } else {
		return crh.getMethods (fqn);
	    }
	}
    }

    public boolean isInterface (String type) {
	TreeNode tn = getType (type);
	if (tn != null)
	    return tn instanceof NormalInterfaceDeclaration;
	TypeParameter tp = nameToType.get (type);
	if (tp != null) {
	    TypeBound b = tp.getTypeBound ();
	    if (b == null)
		return false; // java.lang.Object
	    return isInterface (b.getType ().getFullName ());
	}
	return crh.isInterface (type);
    }

    public void registerTypeParameter (String fqn, TypeParameter tp) {
	String name = "$" + fqn + "$gt$" + tp.getId ();
	nameToType.put (name, tp);
	typeToName.put (tp, name);
    }

    public String getGenericName (TypeParameter tp) {
	return typeToName.get (tp);
    }
}