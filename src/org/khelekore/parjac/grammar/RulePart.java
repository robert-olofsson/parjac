package org.khelekore.parjac.grammar;

import java.util.Collection;
import java.util.Collections;

public class RulePart extends PartBase<String> {

    public RulePart (String rule) {
	super (rule);
    }

    @Override public Collection<String> getSubrules () {
	return Collections.singleton (data);
    }

    @Override public boolean isRulePart () {
	return true;
    }

    @Override public boolean isTokenPart () {
	return false;
    }
}
