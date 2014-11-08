package org.khelekore.parjac.tree;

public class AnnotationUsage implements TreeNode {
    private final String typeName;

    public AnnotationUsage (String typeName) {
	this.typeName = typeName;
    }

    @Override public String toString () {
	return getClass ().getName () + "{typeName: " + typeName + "}";
    }
}