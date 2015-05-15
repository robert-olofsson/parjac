package org.khelekore.parjac.grammar;

import java.util.Collection;

/** A part of a rule that is simple, that is no zero or more, or alternatives (or). */
public interface SimplePart {
    /** The id, rule name or token */
    Object getId ();

    /** Get any rules that this rule contains */
    Collection<String> getSubrules ();

    /** Check if this part is a rule part */
    boolean isRulePart ();

    /** Check if this part is a token part */
    boolean isTokenPart ();
}
