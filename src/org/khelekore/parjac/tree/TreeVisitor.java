package org.khelekore.parjac.tree;

public interface TreeVisitor {
    default boolean visit (CompilationUnit cu) {
	return true;
    }

    /* Visit the types */
    /**
     * @return true to visit subtree, false to stop tree traversal.
     *         The endType will be called no matter what.
     */
    default boolean visit (NormalClassDeclaration c) {
	return true;
    }
    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endType will be called no matter what.
     */
    default boolean visit (EnumDeclaration e) {
	return true;
    }
    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endType will be called no matter what.
     */
    default boolean visit (NormalInterfaceDeclaration i) {
	return true;
    }
    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endType will be called no matter what.
     */
    default boolean visit (AnnotationTypeDeclaration a) {
	return true;
    }
    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endAnonymousClass will be called no matter what.
     */
    default boolean anonymousClass (ClassType ct, ClassBody b) {
	return true;
    }

    default void endAnonymousClass (ClassType ct, ClassBody b) {
    }
    /** Ends the visit of the current type */
    default void endType () {
    }

    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endConstructor will be called no matter what.
     */
    default boolean visit (ConstructorDeclaration c) {
	return true;
    }
    default void endConstructor (ConstructorDeclaration c) {
    }
    default void visit (FieldDeclaration f) {
    }
    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endMethod will be called no matter what.
     */
    default boolean visit (MethodDeclaration m) {
	return true;
    }
    default void endMethod (MethodDeclaration m) {
    }

    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endBlock will be called no matter what.
     */
    default boolean visit (Block b) {
	return true;
    }
    default void endBlock () {
    }
}