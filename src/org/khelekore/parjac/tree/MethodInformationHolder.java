package org.khelekore.parjac.tree;

import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.semantics.MethodInformation;

public interface MethodInformationHolder {
    ParsePosition getParsePosition ();

    void setMethodInformation (MethodInformation actualMethod);
}