package org.khelekore.parjac.semantics;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClassResourceHolder {
    private final Map<String, Result> testedNames =
	Collections.synchronizedMap (new HashMap<> ());

    public boolean hasType (String fqn) {
	Result r = testedNames.get (fqn);
	if (r != null)
	    return r.url != null;

	String name = "/" + fqn.replaceAll ("\\.", "/") + ".class";
	URL u = ClassSetter.class.getResource (name);
	testedNames.put (fqn, new Result (u));
	return u != null;
    }

    private static class Result {
	private URL url;

	public Result (URL url) {
	    this.url = url;
	}
    }
}