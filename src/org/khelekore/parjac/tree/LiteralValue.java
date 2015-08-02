package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.Token;

public interface LiteralValue {
    /** @return the token type */
    Token getLiteralType ();
}
