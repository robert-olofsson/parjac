package org.khelekore.parjac;

import java.nio.file.Path;
import java.util.Comparator;

import javax.tools.Diagnostic;

public class DiagnosticsSorter implements Comparator<Diagnostic<? extends Path>> {

    public int compare (Diagnostic<? extends Path> d1, Diagnostic<? extends Path> d2) {
	if (d1 == null)
	    return d2 == null ? 0 : -1;
	if (d2 == null)
	    return 1;

	if (d1.getClass () != d2.getClass ())
	    return d1.getClass ().getName ().compareTo (d2.getClass ().getName ());
	int k = d1.getKind ().compareTo (d2.getKind ());
	if (k != 0)
	    return k;
	Path p1 = d1.getSource ();
	Path p2 = d2.getSource ();
	if (p1 == null)
	    return p2 == null ? 0 : -1;
	if (p2 == null)
	    return 1;
	int s = p1.compareTo (p2);
	if (s != 0)
	    return s;

	int ln = Long.compare (d1.getLineNumber (), d2.getLineNumber ());
	if (ln != 0)
	    return ln;
	return Long.compare (d1.getColumnNumber (), d2.getColumnNumber ());
    }
}