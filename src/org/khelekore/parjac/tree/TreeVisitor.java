package org.khelekore.parjac.tree;

public interface TreeVisitor {
    void visit (CompilationUnit cu);

    void visit (NormalClassDeclaration c);
    void visit (EnumDeclaration e);
    void visit (NormalInterfaceDeclaration i);
    void visit (AnnotationTypeDeclaration a);
    void endType ();

    void visit (FieldDeclaration f);
    void visit (MethodDeclaration m);
}