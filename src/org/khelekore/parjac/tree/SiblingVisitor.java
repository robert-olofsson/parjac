package org.khelekore.parjac.tree;

/** A tree visitor that only visits things on the same level
 */
public class SiblingVisitor implements TreeVisitor {
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
    @Override public boolean visit (ConstructorDeclaration c) {
	return false;
    }
    @Override public boolean visit (ConstructorBody cb) {
	return false;
    }
    @Override public boolean visit (MethodDeclaration m) {
	return false;
    }
    @Override public boolean visit (Block b) {
	return true;
    }
    @Override public boolean visit (ReturnStatement r) {
	return false;
    }
    @Override public boolean visit (MethodInvocation m) {
	return false;
    }
    @Override public boolean visit (LambdaExpression l) {
	return false;
    }
    @Override public boolean visit (ArrayAccess a) {
	return false;
    }
    @Override public boolean visit (FieldAccess f) {
	return false;
    }
    @Override public boolean visit (IfThenStatement i) {
	return true;
    }
    @Override public boolean visit (WhileStatement w) {
	return false;
    }
    @Override public boolean visit (DoStatement d) {
	return false;
    }
    @Override public boolean visit (BasicForStatement f) {
	return false;
    }
    @Override public boolean visit (EnhancedForStatement f) {
	return false;
    }
    @Override public boolean visit (SwitchStatement s) {
	return false;
    }
    @Override public boolean visit (SwitchBlock s) {
	return false;
    }
    @Override public boolean visit (SwitchBlockStatementGroup s) {
	return false;
    }
    @Override public boolean visit (TryStatement s) {
	return false;
    }
}