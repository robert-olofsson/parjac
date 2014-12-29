package org.khelekore.parjac.grammar;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Rule {
    private final String name;
    private final int id;
    private final List<SimplePart> parts;
    private final int hc;

    public Rule (String name, int id, List<SimplePart> parts) {
	this.name = name;
	this.id = id;
	this.parts = parts;
	hc = name.hashCode () * 31 + parts.hashCode ();
    }

    @Override public String toString () {
	return name + " -> " + parts;
    }

    public String getName () {
	return name;
    }

    public int getId () {
	return id;
    }

    public Collection<String> getSubrules () {
	return parts.stream ().
	    flatMap (p -> p.getSubrules ().stream ()).
	    collect (Collectors.toSet ());
    }

    public List<SimplePart> getParts () {
	return parts;
    }

    public SimplePart getRulePart (int pos) {
	return parts.get (pos);
    }

    public List<SimplePart> getPartsAfter (int pos) {
	return parts.subList (pos, parts.size ());
    }

    public int size () {
	return parts.size ();
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != Rule.class)
	    return false;
	Rule r = (Rule)o;
	// Do not check id. We use this to avoid duplicate rules
	return name.equals (r.name) && parts.equals (r.parts);
    }

    @Override public int hashCode () {
	return hc;
    }
}
