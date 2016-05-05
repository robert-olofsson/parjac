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

    default boolean visit (StaticInitializer s) {
	return true;
    }

    default void endStaticInitializer (StaticInitializer s) {
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
    default boolean visit (ExplicitConstructorInvocation eci) {
	return true;
    }
    default void endExplicitConstructorInvocation (ExplicitConstructorInvocation eci) {
    }

    default boolean visit (FieldDeclaration f) {
	return true;
    }

    default boolean visit (EnumConstant e) {
	return true;
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

    default void visit (LongLiteral l) {
    }

    default void visit (DoubleLiteral d) {
    }

    default void visit (FloatLiteral f) {
    }

    default void visit (StringLiteral s) {
    }

    default void visit (BooleanLiteral b) {
    }

    default void visit (CharLiteral c) {
    }

    default void visit (NullLiteral s) {
    }

    default boolean visit (Assignment a) {
	return true;
    }

    default boolean visit (TernaryExpression t) {
	return true;
    }

    default boolean visit (ClassInstanceCreationExpression c) {
	return true;
    }

    default boolean visit (MethodInvocation m) {
	return true;
    }

    default boolean visit (ArrayCreationExpression ace) {
	return true;
    }

    default void visit (DimExpr de) {
    }

    default boolean visit (LambdaExpression l) {
	return true;
    }

    default void endLambda (LambdaExpression l) {
    }

    default boolean visit (ArrayAccess a) {
	return true;
    }

    default boolean visit (FieldAccess f) {
	return true;
    }

    default void visit (LabeledStatement l) {
    }

    default void endLabel (LabeledStatement l) {
    }

    default void visit (ThrowStatement t) {
    }

    default boolean visit (SynchronizedStatement s) {
	return true;
    }

    default void endSynchronized (SynchronizedStatement s) {
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

    default void endSwitchBlock () {
    }

    default boolean visit (SwitchBlockStatementGroup s) {
	return true;
    }

    default boolean visit (TryStatement t) {
	return true;
    }

    default void visit (Finally f) {
    }

    default void visit (CatchClause c) {
    }

    default boolean visit (CatchType c) {
	return true;
    }

    default void visit (BreakStatement b) {
    }

    default void visit (ContinueStatement b) {
    }

    default boolean visit (LocalVariableDeclaration l) {
	return true;
    }

    default boolean visit (VariableDeclarator v) {
	return true;
    }

    default boolean visit (CastExpression c) {
	return true;
    }

    default void visit (Identifier i) {
    }

    default void visit (DottedName d) {
    }

    default void visit (UnaryExpression u) {
    }

    default boolean visit (TwoPartExpression t) {
	return true;
    }

    default boolean visit (PreIncrementExpression p) {
	return true;
    }

    default boolean visit (PostIncrementExpression p) {
	return true;
    }

    default boolean visit (PreDecrementExpression p) {
	return true;
    }

    default boolean visit (PostDecrementExpression p) {
	return true;
    }

    default void visit (PrimaryNoNewArray.ThisPrimary t) {
    }

    default void visit (PrimaryNoNewArray.DottedThis t) {
    }

    default void visit (PrimaryNoNewArray.VoidClass t) {
    }

    default boolean visit (PrimaryNoNewArray.ClassPrimary t) {
	return true;
    }
}
