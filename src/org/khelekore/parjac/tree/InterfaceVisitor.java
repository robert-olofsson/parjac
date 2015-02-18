package org.khelekore.parjac.tree;

public interface InterfaceVisitor {
    void visit (SingleTypeImportDeclaration i);
    void visit (TypeImportOnDemandDeclaration i);
    void visit (SingleStaticImportDeclaration i);
    void visit (StaticImportOnDemandDeclaration i);
}