package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;

public interface ImportDeclaration {
    void visit (ImportVisitor iv);

    /** Mark this import clause as used */
    void markUsed ();

    /** Check if this import clause is actually used */
    boolean hasBeenUsed ();

    /** Get the parse position for this import */
    ParsePosition getParsePosition ();
}