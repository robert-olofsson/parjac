package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;

public class ModifierTokenType extends TokenTypeBase {
    public ModifierTokenType (Token type, ParsePosition pos) {
	super (type, pos);
    }
}