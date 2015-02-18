package org.khelekore.parjac.tree;

public interface TreeVisitor {
    default void visit (CompilationUnit cu) {
    }

    /* Visit the types */
    default void visit (NormalClassDeclaration c) {
    }
    default void visit (EnumDeclaration e) {
    }
    default void visit (NormalInterfaceDeclaration i) {
    }
    default void visit (AnnotationTypeDeclaration a) {
    }
    default void anonymousClass (ClassBody b) {
    }

    /** Ends the visit of the current type */
    default void endType () {
    }

    default void visit (ConstructorDeclaration c) {
    }
    default void visit (FieldDeclaration f) {
    }
    default void visit (MethodDeclaration m) {
    }

    default void visit (Block b) {
    }
    default void endBlock () {
    }
}