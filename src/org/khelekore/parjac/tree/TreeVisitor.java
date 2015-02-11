package org.khelekore.parjac.tree;

public interface TreeVisitor {
    void visit (CompilationUnit cu);

    /* Visit the types */
    void visit (NormalClassDeclaration c);
    void visit (EnumDeclaration e);
    void visit (NormalInterfaceDeclaration i);
    void visit (AnnotationTypeDeclaration a);
    void anonymousClass (ClassBody b);

    /** Ends the visit of the current type */
    void endType ();

    void visit (ConstructorDeclaration c);
    void visit (FieldDeclaration f);
    void visit (MethodDeclaration m);

    void visit (Block b);
    void endBlock ();
}