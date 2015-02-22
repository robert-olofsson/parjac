package org.khelekore.parjac.tree;

public interface ImportVisitor {
    void visit (SingleTypeImportDeclaration i);
    void visit (TypeImportOnDemandDeclaration i);
    void visit (SingleStaticImportDeclaration i);
    void visit (StaticImportOnDemandDeclaration i);
}