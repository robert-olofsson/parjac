package org.khelekore.parjac.tree;

public interface TreeVisitor {
    default boolean visit (CompilationUnit cu) {
	return true;
    }

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
    /** Ends the visit of the current type */
    default void endType () {
    }
    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endAnonymousClass will be called no matter what.
     */
    default boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	return true;
    }

    default void endAnonymousClass (ClassType ct, ClassBody b) {
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
    default boolean visit (ConstructorBody cb) {
	return true;
    }
    default void visit (ExplicitConstructorInvocation eci) {
    }
    default void endExplicitConstructorInvocation (ExplicitConstructorInvocation eci) {
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

    default void visit (MethodBody b) {
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

    /**
     * @return true to visit subtree, false to stop tree traversal
     *         The endReturn will be called no matter what.
     */
    default boolean visit (ReturnStatement r) {
	return true;
    }
    default void endReturn (ReturnStatement r) {
    }

    default void visit (IntLiteral i) {
    }

    default void visit (DoubleLiteral d) {
    }

    default void visit (FloatLiteral f) {
    }

    default void visit (StringLiteral s) {
    }

    default void visit (BooleanLiteral b) {
    }

    default void visit (NullLiteral s) {
    }

    default void visit (Assignment a) {
    }

    default void visit (ClassInstanceCreationExpression c) {
    }

    default boolean visit (MethodInvocation m) {
	return true;
    }

    default boolean visit (LambdaExpression l) {
	return true;
    }

    default boolean visit (ArrayAccess a) {
	return true;
    }

    default boolean visit (FieldAccess f) {
	return true;
    }

    default void visit (LabeledStatement l) {
    }

    default void visit (ThrowStatement t) {
    }

    default void visit (SynchronizedStatement s) {
    }

    default boolean visit (IfThenStatement i) {
	return true;
    }

    default boolean visit (WhileStatement w) {
	return true;
    }

    default boolean visit (DoStatement d) {
	return true;
    }

    default boolean visit (BasicForStatement f) {
	return true;
    }

    default boolean visit (EnhancedForStatement f) {
	return true;
    }

    default void endFor () {
    }

    default boolean visit (SwitchStatement s) {
	return true;
    }

    default boolean visit (SwitchBlock s) {
	return true;
    }

    default boolean visit (SwitchBlockStatementGroup s) {
	return true;
    }

    default boolean visit (TryStatement s) {
	return true;
    }

    default void visit (Finally f) {
    }

    default void visit (CatchClause c) {
    }

    default void visit (BreakStatement b) {
    }

    default void visit (LocalVariableDeclaration l) {
    }

    default void visit (CastExpression c) {
    }
}