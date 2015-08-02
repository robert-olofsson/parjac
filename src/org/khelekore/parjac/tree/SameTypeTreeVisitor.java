package org.khelekore.parjac.tree;

/** A tree visitor that does not go into types.
 *  Useful when traversing statements inside a method.
 */
public class SameTypeTreeVisitor implements TreeVisitor {
    /* Do not look into classes */
    @Override public boolean visit (NormalClassDeclaration c) {
	return false;
    }
    @Override public boolean visit (EnumDeclaration e) {
	return false;
    }
    @Override public boolean visit (NormalInterfaceDeclaration i) {
	return false;
    }
    @Override public boolean visit (AnnotationTypeDeclaration a) {
	return false;
    }
    @Override public boolean anonymousClass (ClassType ct, ClassBody b) {
	return false;
    }
}