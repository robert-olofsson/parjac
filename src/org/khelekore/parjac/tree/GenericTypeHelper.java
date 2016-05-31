package org.khelekore.parjac.tree;

public class GenericTypeHelper {
    public static String getGenericType (TreeNode tn) {
	if (tn instanceof ClassType) {
	    ClassType ct = (ClassType)tn;
	    TypeParameter tp = ct.getTypeParameter ();
	    if (tp != null)
		return "T" + tp.getId () + ";";

	    TypeArguments ta = ct.getTypeArguments ();
	    if (ta != null)
		return "L" + ct.getSlashName () + getTypeArgumentsSignature (ta) + ";";
	}
	return tn.getExpressionType ().getDescriptor ();
    }

    private static String getTypeArgumentsSignature (TypeArguments ta) {
	if (ta == null)
	    return "";
	StringBuilder sb = new StringBuilder ();
	sb.append ("<");
	for (TreeNode tn : ta.getTypeArguments ()) {
	    sb.append (getGenericType (tn));
	}
	sb.append (">");
	return sb.toString ();
    }
}
